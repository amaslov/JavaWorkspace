#/*
# * TFTP library 
# * copyright (c) 2004 Vanden Berghen Frank  
# *
# * 
# * This library is free software; you can redistribute it and/or
# * modify it under the terms of the GNU Lesser General Public
# * License version 2.1 as published by the Free Software Foundation
# * 
# * This library is distributed in the hope that it will be useful,
# * but WITHOUT ANY WARRANTY; without even the implied warranty of
# * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# * Lesser General Public License for more details.
# * 
# * You should have received a copy of the GNU Lesser General Public
# * License along with this library; if not, write to the Free Software
# * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
# * 
# */
#
#makefile for the TFTP tools
#
all : sserver sclient

ifeq ($(MACHTYPE),sun_sparc)
LIBRARIES=-lsocket -lnsl 
endif

ifeq ($(MACHTYPE),i386)
LIBRARIES=
endif

sserver : sserver.cpp tftp.cpp open_sok.cpp tftp.h
	g++ -g -Wall -D_REENTRANT -lpthread $(LIBRARIES) -o tftpd sserver.cpp tftp.cpp open_sok.cpp
sclient : sclient.cpp tftp.cpp open_sok.cpp tftp.h
	g++ -g -Wall $(LIBRARIES) -o tftp sclient.cpp tftp.cpp open_sok.cpp

clean:
	rm tftpd
	rm tftp
	rm *~
