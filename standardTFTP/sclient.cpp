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

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <memory.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include "tftp.h"

int TimeOut,NumberTimeOut,PortTFTP;

int creer_socket(int type, int *ptr_port, struct sockaddr_in *ptr_adresse);

void tftp_connection(struct sockaddr_in *adresse,char *job,char *name, char *distname)
{
    FILE *f;
    int r=255;

    if ((job[0]=='S')||(job[0]=='s'))
    { 
        f=fopen(name,"rb");
        if (distname==NULL) r=tftp_send(adresse,name,"octet",1,TFTPsread,f);
        else r=tftp_send(adresse,distname,"octet",1,TFTPsread,f);
        fclose(f);
    };
    if ((job[0]=='R')||(job[0]=='r')) 
    {
        f=fopen(name,"wb");
        if (distname==NULL) r=tftp_receive(adresse,name,"octet",1,TFTPswrite,f);
        else r=tftp_receive(adresse,distname,"octet",1,TFTPswrite,f);
        fclose(f);
    };
    if (r!=0) printf("error tftp.\n\n");
};

int main(int argc,char **argv)
{
    struct hostent *hp;          /* pour l'adresse de la machine distante */
    struct sockaddr_in adresse_serveur;

    TimeOut=7;
    NumberTimeOut=3;

    if (argc<4)
    {
    	fprintf(stderr,"SYNTAX: tftpc <server_name> <server_port> <S|R> <file_name> [<dist_file_name>]\n\n"
    				   "<server_name>    : name of the server.\n"
    				   "<server_port>    : initial accept port.\n"
    				   "<S|R>            : Send or Receive.\n"
    				   "<file_name>      : name of the file to Send or Receive.\n"
    				   "<dist_file_name> : name of the file on the distant machine (optional).\n"
    				   "All transfers are binary. If the file already exists, it will be replaced.\n\n");
    	exit(2);
  	};
  
    /* recherche de l'adresse internet de la machine du serveur */
    if ((hp=gethostbyname(argv[1]))==NULL)
    {
        fprintf(stderr,"machine %s inconnue.\n",argv[1]);
        exit(2); 
    };

    PortTFTP=atol(argv[2]);
    if (PortTFTP==0)
    {
        fprintf(stderr,"invalid port number.\n");
        exit(3); 
    };

    /* preparation de l'adresse du serveur */
    adresse_serveur.sin_family=AF_INET;
    adresse_serveur.sin_port=htons(PortTFTP);
    memcpy(&adresse_serveur.sin_addr.s_addr, hp->h_addr, hp->h_length);
    if (argc==6) tftp_connection(&adresse_serveur,argv[3],argv[4],argv[5]);
    else tftp_connection(&adresse_serveur,argv[3],argv[4],NULL);
    return 0;
};
