//
//  flcudpsocket.c
//  cordova-plugin-flc-udpsocket-iostest
//
//  Created by wojcieszki on 09/04/2018.
//  Copyright © 2018 wojcieszki. All rights reserved.
//

#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <net/if.h>
#include <sys/types.h>
#include <string.h>
#include <unistd.h>
#include <netdb.h>
#include <ifaddrs.h>

#import "flcudpsocket.h"

int flc_udpsocket_create() {
  int socketfd = socket(AF_INET, SOCK_DGRAM, 0);
  int r = -1, s = -1, n = -1;
  
  int broadcaston = 1;
  int reuseon = 1;
  int nosigpipeon = 1;
  r = setsockopt(socketfd, SOL_SOCKET, SO_BROADCAST, &broadcaston, sizeof(broadcaston));
  s = setsockopt(socketfd, SOL_SOCKET, SO_REUSEADDR, &reuseon, sizeof(reuseon));
  n = setsockopt(socketfd, SOL_SOCKET, SO_NOSIGPIPE, &nosigpipeon, sizeof(nosigpipeon));
  
  if (r == 0 & s == 0 & n == 0) {
    return socketfd;
  } else {
    return -1;
  }
}

int flc_udpsocket_bind(int socketfd, int port) {
  struct sockaddr_in serv_addr;
  memset( &serv_addr, '\0', sizeof(serv_addr));
  serv_addr.sin_len = sizeof(struct sockaddr_in);
  serv_addr.sin_family = AF_INET;
  serv_addr.sin_port = htons(port);
  serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
  return bind(socketfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr));
}

int flc_udpsocket_receive(int socketfd, char *outdata, int expted_len, char *remoteip, int *remoteport) {
  struct sockaddr_in cli_addr;
  socklen_t clilen = sizeof(cli_addr);
  memset(&cli_addr, 0x0, sizeof(struct sockaddr_in));
  int len = (int)recvfrom(socketfd, outdata, expted_len, 0, (struct sockaddr *)&cli_addr, &clilen);
  char *clientip = inet_ntoa(cli_addr.sin_addr);
  memcpy(remoteip, clientip, strlen(clientip));
  *remoteport = ntohs(cli_addr.sin_port);
  return len;
}

int flc_udpsocket_close(int socketfd) {
  return close(socketfd);
}

int flc_udpsocket_sendto(int socketfd, const char *msg, int len, const char *toaddr, int toport) {
  struct sockaddr_in address;
  memset(&address, 0x0, sizeof(struct sockaddr_in));
  address.sin_family = AF_INET;
  address.sin_port = htons(toport);
  address.sin_addr.s_addr = inet_addr(toaddr);
  int sendlen = (int)sendto(socketfd, msg, len, 0, (struct sockaddr *)&address, sizeof(address));
  
  return sendlen;
}

int flc_udpsocket_broadcast(int socketfd, const char *msg, int len, const char *address, int toport) {
  char broadcast_address[NI_MAXHOST];
  int r = flc_udpsocket_get_broadcast_address(broadcast_address);
  if (r == -1) {
    return -1;
  }
  
  struct sockaddr_in send_address;
  memset(&send_address, 0x0, sizeof(struct sockaddr_in));
  send_address.sin_family = AF_INET;
  send_address.sin_port = htons(toport);
  inet_pton(AF_INET, broadcast_address, &send_address.sin_addr);
  int sendlen = (int)sendto(socketfd, msg, len, 0, (struct sockaddr *)&send_address, sizeof(send_address));
  
  return sendlen;
}

int flc_udpsocket_get_broadcast_address(char* broadcast_address) {
  struct ifaddrs address;
  int r = flc_udpsocket_get_ifaddrs(&address);
  if (r != 0) {
    printf("flc_udpsocket_get_ifaddrs() failed\n"); 
    return -1;
  }
  
  char ip[NI_MAXHOST], netmask[NI_MAXHOST];
  int s = getnameinfo(address.ifa_addr, sizeof(struct sockaddr_in), ip, NI_MAXHOST, NULL, 0, NI_NUMERICHOST);
  if (s != 0) {
    printf("getnameinfo() failed: %s\n", gai_strerror(s));
    return -1;
  }
  
  s = getnameinfo(address.ifa_netmask, sizeof(struct sockaddr_in), netmask, NI_MAXHOST, NULL, 0, NI_NUMERICHOST);
  if (s != 0) {
    printf("getnameinfo() failed: %s\n", gai_strerror(s));
    return -1;
  }
  
  struct in_addr host, mask, broadcast;
  if (inet_pton(AF_INET, ip, &host) == 1 && inet_pton(AF_INET, netmask, &mask) == 1) {
    broadcast.s_addr = host.s_addr | ~mask.s_addr;
  } else {
    return -1;
  }
  if (inet_ntop(AF_INET, &broadcast, broadcast_address, INET_ADDRSTRLEN) != NULL) {
    return 0;
  } else {
    return -1;
  }
}

int flc_udpsocket_get_ifaddrs(struct ifaddrs *address) {
  struct ifaddrs *ifaddr, *ifa;
  int family, n;
  
  if (getifaddrs(&ifaddr) == -1) {
    perror("getifaddrs");
    return -1;
  }
   
  int ifOk = -1;
  
  for (ifa = ifaddr, n = 0; ifa != NULL; ifa = ifa->ifa_next, n++) {
    if (ifa->ifa_addr == NULL) continue;
    if (ifa->ifa_flags & IFF_LOOPBACK) continue;
    if (!(ifa->ifa_flags & IFF_BROADCAST) || ifa->ifa_dstaddr == NULL) continue;
    
    family = ifa->ifa_addr->sa_family;
    if (family != AF_INET) continue;
    
    if (strcmp(ifa->ifa_name, "bridge100") == 0) {
      memcpy(address, ifa, sizeof(struct ifaddrs));
      ifOk = 0;
      break;
    }
    
    if (strcmp(ifa->ifa_name, "en0") == 0) {
      memcpy(address, ifa, sizeof(struct ifaddrs));
      ifOk = 0;
    }
  }

  if (ifOk == -1) {
      printf("No valid interface found\n");
      return -1;
  }
  
  freeifaddrs(ifaddr);
  return 0;
}

int flc_udpsocket_get_error(int socket_fd) {
  int error_code;
  int error_code_size = sizeof(error_code);
  getsockopt(socket_fd, SOL_SOCKET, SO_ERROR, &error_code, &error_code_size);
  if (error_code != 0) {
    printf("flc_udpsocket_get_error error_code = %d\n", error_code);
  }
  return error_code;
}
