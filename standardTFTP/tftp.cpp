/*
 * TFTP library 
 * copyright (c) 2004 Vanden Berghen Frank  
 *
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1 as published by the Free Software Foundation
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

#include <sys/types.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/ioctl.h>
#include <netinet/in.h>
#include <syslog.h>
#include <stdio.h>   
#include <stdlib.h>
#include <memory.h>
#include <unistd.h>
#include <stropts.h>
#include "tftp.h"

extern int TimeOut,NumberTimeOut;

#ifdef __sun__
#define FIONREAD I_NREAD
#endif

#define MIN(a,b) ((a)<(b)?(a):(b))

char TFTPswrite(char *data,long n,char first,void *f)
{
    fwrite(data,n,1,(FILE *)f);
    return 0;
};

char TFTPsread(char *data,long *n,char first,void *f)
{
    *n=fread(data,1,SEGSIZE,(FILE *)f);
    return 0;
};

typedef struct TFTParg_buffer_tag
        {
            char *dat;
            long l;
        } TFTParg_buffer;

char TFTPsSendBuffer(char *data,long *s,char first,void *a)
{
    long size=MIN(PKTSIZE-4,((TFTParg_buffer *)a)->l);
    *s=size;
    memcpy(data,((TFTParg_buffer *)a)->dat,size);
    ((TFTParg_buffer *)a)->l-=size; ((TFTParg_buffer *)a)->dat+=size;
    return 0;
};

int creer_socket(int type, int *ptr_port, struct sockaddr_in *ptr_adresse);

//char TFTPwrite(char *data,long n,char first,void *argu);
//char TFTPread(char *data,long *n,char first,void *argu);

void nak(int peer,struct sockaddr_in *to,int error,char *commentaire)
{
   char buf[PKTSIZE];
   struct tftphdr *tp=(tftphdr *)&buf;
   int length;
   size_t tolen=sizeof(to);

   syslog(LOG_ERR,commentaire);
   tp->th_opcode = htons((u_short)ERROR);
   tp->th_code = htons((u_short)error);
   strcpy(tp->th_msg, commentaire);
   length = strlen(tp->th_msg);
   tp->th_msg[length] = '\0';
   length += 5;
   if (sendto(peer, buf, length, 0,(struct sockaddr*)to,tolen) != length) syslog(LOG_ERR, "nak: %m\n");
};

int tftp_receive_ext(struct sockaddr_in *to1,char *name,char *mode,int InClient,                
                     char (*TFTPwrite)(char *,long ,char,void *),
                     void *argu,int vPKTSIZE)
{
    char *buf,*ackbuf,*dat,*cp;
    tftphdr *dp,*ap;
    int i,size,n,ntimeout,peer;
    struct timeval tv;
    u_short nextBlockNumber;
    fd_set lecture;
    struct sockaddr_in from,to=*to1;
    size_t fromlen=sizeof(from),tolen=fromlen;
    
    buf=(char*)malloc(vPKTSIZE);
    if (buf==NULL)
    {
        fprintf(stderr,"TFTP: out of memory.\n");
        return 255;
    };
    ackbuf=(char*)malloc(vPKTSIZE);
    if (ackbuf==NULL)
    {
        fprintf(stderr,"TFTP: out of memory.\n");
        free(buf);
        return 255;
    };
    dp=(tftphdr *)buf;
    ap=(tftphdr *)ackbuf;
    dat=(char*)&dp->th_data[0];
    cp=(char*)&ap->th_stuff[0];

    i=0;
    if ((peer=creer_socket(SOCK_DGRAM, &i, NULL))<0)
    {
        syslog(LOG_ERR,"creation socket client: %m\n");
        free(buf); free(ackbuf);
        return 255;
    };
          
    if (InClient)
    {
        ap->th_opcode=htons((u_short)RRQ);
        strcpy(cp, name);
    	cp += strlen(name);
	    *cp++ = '\0';
	    strcpy(cp, mode);
    	cp += strlen(mode);
    	*cp++ = '\0';
        size=(DWORD)cp-(DWORD)ackbuf;
    } else
    {
        ap->th_opcode=htons((u_short)ACK);
        ap->th_block=0;
        size=4;
    };
    nextBlockNumber=1;
    
    do 
    {    
      ntimeout=0;
      do
      {
         if (ntimeout==NumberTimeOut) { close(peer); free(buf); free(ackbuf); return 255;}

         if (sendto(peer,ap,size,0,(struct sockaddr *)&to,tolen)!=size)
         {
             syslog(LOG_ERR, "tftp: write: %m\n");
             close(peer);  free(buf); free(ackbuf);
             return 255;
         }

         do
         {
             n=-1;
	         FD_ZERO(&lecture);
	         FD_SET(peer,&lecture); 
	         tv.tv_sec=TimeOut; tv.tv_usec=0;
	         if ((i=select(peer+1, &lecture, NULL, NULL, &tv))==-1)
	         {
	            syslog(LOG_ERR,"erreur select.\n");
	            close(peer); free(buf); free(ackbuf);
	            return 255;
	         };
	         if (i>0) n=recvfrom(peer, dp, vPKTSIZE, 0,(struct sockaddr *)&from, &fromlen);
	     } while ((n<0)&&(i>0));

         if (i>0)
         {
            to.sin_port=from.sin_port;
            dp->th_opcode = ntohs((u_short)dp->th_opcode);            
            dp->th_block = ntohs((u_short)dp->th_block);
            if (dp->th_opcode != DATA) 
            {
                close(peer); free(buf); free(ackbuf);
                return 255;
            };
        
            if (dp->th_block != nextBlockNumber)
            {
               /* Re-synchronize with the other side */
               ioctl(peer, FIONREAD, &i); //i=number of byte in read-buffer
               while (i)
               {
                  recv(peer, dp, vPKTSIZE, 0);
                  ioctl(peer, FIONREAD, &i);
               };
               dp->th_block=nextBlockNumber+1;
            };
         };
         ntimeout++;
      } while (dp->th_block!=nextBlockNumber);

      ap->th_block=htons(nextBlockNumber);
      nextBlockNumber++;

      if (nextBlockNumber==2)
      {
          ap->th_opcode=htons((u_short)ACK); // seulement utile si InClient=1
          size=4;
      };

// les données sont dans dat et leur longueur est de n-4
      if (n-4>0)
      {
          if (nextBlockNumber==2) i=(*TFTPwrite)(dat,n-4,1,argu);
          else i=(*TFTPwrite)(dat,n-4,0,argu);
          if (i)
          {
               close(peer); free(buf); free(ackbuf);
               return i;
          };
      };
 
   } while (n == vPKTSIZE);

   /* send the "final" ack */
   sendto(peer, ap, 4, 0,(struct sockaddr *)&to,tolen);
   close(peer); free(buf); free(ackbuf);
   return 0;
};

int tftp_receive(struct sockaddr_in *to1,char *name,char *mode,int InClient,                
                 char (*TFTPwrite)(char *,long ,char,void *),
                 void *argu)
{
    return tftp_receive_ext(to1,name,mode,InClient,TFTPwrite,argu,PKTSIZE);
};

int tftp_send_ext(struct sockaddr_in *to1,char *name,char *mode,int InClient,
                  char (*TFTPread)(char *,long *,char,void *),                
                  void *argu, int vPKTSIZE)
{
    char *buf,*ackbuf,*dat,*cp;
    tftphdr *dp,*ap;
    int i,size,Oldsize=vPKTSIZE,n,ntimeout,peer;
    ushort nextBlockNumber;
    struct timeval tv;
    fd_set lecture;
    struct sockaddr_in from,to=*to1;
	size_t fromlen=sizeof(from),tolen=fromlen;

    buf=(char*)malloc(vPKTSIZE);
    if (buf==NULL)
    {
        fprintf(stderr,"TFTP: out of memory.\n");
        return 255;
    };
    ackbuf=(char*)malloc(vPKTSIZE);
    if (ackbuf==NULL)
    {
        fprintf(stderr,"TFTP: out of memory.\n");
        free(buf); return 255;
    };
    dp=(tftphdr *)buf;
    ap=(tftphdr *)ackbuf;
    dat=(char*)&dp->th_data[0];
    cp=(char*)&dp->th_stuff[0];

    i=0;
    if ((peer=creer_socket(SOCK_DGRAM, &i, NULL))<0)
    {
        syslog(LOG_ERR,"creation socket client: %m\n");
        free(buf); free(ackbuf);
        return 255;
    };
    
    if (InClient)
    {
        dp->th_opcode=htons((u_short)WRQ);
        strcpy(cp, name);
    	cp += strlen(name);
	    *cp++ = '\0';
	    strcpy(cp, mode);
    	cp += strlen(mode);
    	*cp++ = '\0';
        size=(DWORD)cp-(DWORD)buf;
        nextBlockNumber=0;
    } else
    {
        dp->th_opcode=htons((u_short)DATA);
        dp->th_block=htons((ushort)1);
        if ((*TFTPread)(dat,(long*)(&size),1,argu)!=0)
        {
            close(peer); free(buf); free(ackbuf);
            return 255;
        }; 
        size+=4;
        nextBlockNumber=1;
    };

    do 
    {    
      ntimeout=0;
      do
      {
         if (ntimeout==NumberTimeOut) { close(peer); free(buf); free(ackbuf); return 255;}

         if (sendto(peer,dp,size,0,(struct sockaddr *)&to,tolen)!=size)
         {
             syslog(LOG_ERR, "tftp: write: %m\n");
             close(peer); free(buf); free(ackbuf);
             return 255;
         };
         
         do
         {
             n=-1;
	         FD_ZERO(&lecture);
	         FD_SET(peer,&lecture); 

	         tv.tv_sec=TimeOut; tv.tv_usec=0;
	         if ((i=select(peer+1, &lecture, NULL, NULL, &tv))==-1)
	         {
	            syslog(LOG_ERR,"erreur select.\n");
	            close(peer); free(buf); free(ackbuf);
	            return 255;
	         };

	         if (i>0) // = time out not expired
	            n=recvfrom(peer, ap, vPKTSIZE, 0,(struct sockaddr *)&from, &fromlen);                
         } while ((n<0)&&(i>0));

         if (i>0)
         {
            to.sin_port=from.sin_port;
	        ap->th_opcode = ntohs((u_short)ap->th_opcode);
            ap->th_block = ntohs((u_short)ap->th_block);
            if (ap->th_opcode != ACK) { close(peer); free(buf); free(ackbuf); return 255;}

            if (ap->th_block != nextBlockNumber)
            {
               /* Re-synchronize with the other side */
               ioctl(peer, FIONREAD, &i); //i=number of byte in read-buffer
               while (i)
               {
                  recv(peer, ap, vPKTSIZE, 0);
                  ioctl(peer, FIONREAD, &i);
               };
               ap->th_block=nextBlockNumber+1;
            };
         };
         ntimeout++;
      } while (ap->th_block!=nextBlockNumber);

      if ((size<vPKTSIZE)&&(nextBlockNumber!=0)) break; // tout a déjà été envoyé dans le 1er packet de DATA
        //  attention pour ce test si on fait le "tour" du ushort !!

      nextBlockNumber++;
      dp->th_block=htons(nextBlockNumber);
      if (nextBlockNumber==1)
      {
         dp->th_opcode=htons((u_short)DATA); // seulement utile si InClient=1
         i=(*TFTPread)(dat,(long*)(&size),1,argu);
      } else 
      {
         Oldsize=size;
         if (Oldsize==vPKTSIZE) i=(*TFTPread)(dat,(long*)(&size),0,argu);
         else i=0;
      };
      if (i)
      {
         close(peer); free(buf); free(ackbuf);
         return i;
      };

      size+=4;
   } while (Oldsize==vPKTSIZE);
   close(peer); free(buf); free(ackbuf);
   return 0;
};

int tftp_send(struct sockaddr_in *to1,char *name,char *mode,int InClient,
              char (*TFTPread)(char *,long *,char,void *),                
              void *argu)
{
    return tftp_send_ext(to1,name,mode,InClient,TFTPread,argu,PKTSIZE);
};

int tftp_send_buffer(struct sockaddr_in *to,char *name,char *mode,char *datas,DWORD l)
{
    TFTParg_buffer a;
    a.dat=datas;
    a.l=l;
    return tftp_send(to,name,mode,1,TFTPsSendBuffer,&a);
};
