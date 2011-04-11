/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.log4j.Logger;

import com.globalros.tftp.common.ERROR;
import com.globalros.tftp.common.FRQ;
import com.globalros.tftp.common.RRQ;
import com.globalros.tftp.common.VirtualFile;
import com.globalros.tftp.common.VirtualFileImpl;
import com.globalros.tftp.common.VirtualFileSystem;
import com.globalros.tftp.common.WRQ;

/**
 * class:   TFTPRequestHandler
 * package: com.globalros.tftp.server
 * project: tftp4java
 */
/**
 * @author  marco
 */

/*
 * This class handles off one TFTP write or read request
 * in future time this could be improved to handle more than one
 * request on same socket.
 * Thus the maximum of 65534 ports could be overcome
 * if capacity on NIC and processor would allow. */
public class TFTPRequestHandler
{
    /**
     * store reference to file system for later use
     */
    private VirtualFileSystem vfs = null;

    /**
     * event listener on which we can fire events
     */
    private EventListener listener = null;

    /** Creates a new instance of TFTPRequestHandler */
    public TFTPRequestHandler(VirtualFileSystem vfs, EventListener listener) throws SocketException
    {
        this.vfs = vfs;
        this.listener = listener;
        tftpClient = new ClientHandler();
        //		mcpq = new MCPStream();
        /* would like to replace with dynamic array! */
        try
        {
            ctx = new InitialContext();
        }
        catch (javax.naming.NamingException e)
        {
            tftpLog.warn("Could not create InitialContext! " + e.toString());
            throw new SocketException("Cannot get new InitialContext for TFTPRequesHandler, run would fail!");
        }
    }

    /** 
     * This method is called when a client sends another WRQ or RRQ while
     * this TFTPRequestHandler is already working on a previous WRQ or RRQ
     * from the client
     */
    public boolean waitingForNewRequest(FRQ frq)
    {
        if (tftpClient.waitingForNewRequest())
        {
            tftpClient.newRequest();
            return true;
        }
        else
        {
            return false;
        }
    }

    // This run is called as main for TFTPWorkerThread it is passed a packet
    // received on the TFTPServerSocket
    public void run(FRQ frq, InetAddress clientAddress, int clientPort)
    {
        if (frq == null)
        {
            tftpLog.error("TFTPRequestHandler run is called with null packet!");
            return;
        }
        if (clientAddress == null)
        {
            tftpLog.error("TFTPRequestHandler run is called with invalid client address!");
            return;
        }
        if (clientPort == 0)
        {
            tftpLog.error("TFTPRequestHandler run is called with invalid client port!");
            return;
        }

        // prepare the tftpClient to send packages to the new client
        tftpClient.connect(clientAddress, clientPort);

        // get timeout otherwise set default to 5 secs
        int timeout = frq.getTimeout();
        if (timeout <= 0)
            timeout = 5;
        tftpClient.setTimeout(timeout);

        // get tsize otherwise set default to half megabyte
        int tsize = frq.getTransferSize();
        if (tsize < 0)
            tsize = 512 * 1024;
        tftpClient.setTransferSize(tsize);

        thisThread = Thread.currentThread();

        if (frq instanceof RRQ)
        {
            RRQ rrq = (RRQ) frq;
            tftpLog.debug(tftpClient.getClient() + " RRQ " + rrq.getFileName());
            boolean sendOK = false;
            VirtualFile file = new VirtualFileImpl(rrq.getFileName());

            InputStream is = null;
            try
            {
                // get an InputStream from VirtualFileSystem and read
                // within the ClientHandler from this stream
                is = vfs.getInputStream(file);
            }
            catch (FileNotFoundException e)
            {
                tftpLog.info("FileNotFoundException: " + e.getMessage());
                tftpClient.sendErrPacket(ERROR.ERR_FILE_NOT_FOUND, e.getMessage());
                return;
            }

            // at this point we have valid pizza stream

            // retrieve mcp data sa fileContent from mcpServer
            sendOK = tftpClient.sendFileToClient(is /*fileContent*/
            , clientAddress, clientPort, frq.hasOptions());

            // Generate after download event
            if (listener != null)
                listener.onAfterDownload(clientAddress, clientPort, rrq.getFileName(), sendOK);

            try
            {
                is.close();
            }
            catch (IOException e)
            {
                tftpLog.warn("This is sad but true, we cannot close the inputStream for " + rrq.getFileName());
                return;
            }
        }
        else if (frq instanceof WRQ)
        {
            WRQ wrq = (WRQ) frq;
            tftpLog.debug(tftpClient.getClient() + " WRQ " + wrq.getFileName());
            VirtualFile file = new VirtualFileImpl(wrq.getFileName());

            // retrieve file from client
            try
            {
                // get an OutputStream from VirtualFileSystem and write to it
                // within the ClientHandler from this stream
                OutputStream os = vfs.getOutputStream(file);
                //				fileContent =
                boolean receiveOK = tftpClient.receiveFileFromClient(os, clientAddress, clientPort, frq.hasOptions());
                // FDU: receiveFileFromClient has already closed the stream
                //os.close();
                if (listener != null)
                    listener.onAfterUpload(clientAddress, clientPort, wrq.getFileName(), receiveOK);
            }
            catch (Exception e)
            {
                tftpLog.debug("Exception occurred in tftp.run " + e);
                e.printStackTrace();
            }
        }
    }

    public void stop()
    {
        // disconnect the client from here that no strange packets are sent to client
        tftpClient.disconnect();
    }
    
    Thread thisThread;
    Context ctx;
    ClientHandler tftpClient;
    //	MCPStream mcpq;
    static Logger tftpLog = Logger.getLogger(TFTPRequestHandler.class);
}
