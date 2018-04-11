//
//  flcudpsocket.h
//  cordova-plugin-flc-udpsocket-iostest
//
//  Created by wojcieszki on 09/04/2018.
//  Copyright © 2018 wojcieszki. All rights reserved.
//

#ifndef flcudpsocket_h
#define flcudpsocket_h

int flc_udpsocket_create();
int flc_udpsocket_bind(int socketfd, int port);
int flc_udpsocket_receive(int socketfd, char *outdata, int expted_len, char *remoteip, int *remoteport);
int flc_udpsocket_close(int socketfd);
int flc_udpsocket_sendto(int socketfd, const char *msg, int len, const char *toaddr, int toport);
int flc_udpsocket_broadcast(int socketfd, const char *msg, int len, const char *address, int toport);
int flc_udpsocket_get_broadcast_address(char* broadcast_address);
int flc_udpsocket_get_ifaddrs(struct ifaddrs *address);

#endif /* flcudpsocket_h */
