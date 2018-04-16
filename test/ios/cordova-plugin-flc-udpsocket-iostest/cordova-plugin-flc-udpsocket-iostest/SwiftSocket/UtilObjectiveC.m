#import <Foundation/Foundation.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <ifaddrs.h>
#import "UtilObjectiveC.h"

@implementation UtilObjectiveC

+ (NSDictionary *)getIPAddresses {
  const NSString *WIFI_IF = @"en0";
  const NSString *HOTSPOT_IF = @"bridge100";
  NSArray *KNOWN_WIRED_IFS = @[@"en2",@"en3",@"en4"];
  NSArray *KNOWN_CELL_IFS = @[@"pdp_ip0",@"pdp_ip1",@"pdp_ip2",@"pdp_ip3"];
  
  const NSString *UNKNOWN_IP_ADDRESS = @"";
  
  NSMutableDictionary *addresses = [NSMutableDictionary dictionaryWithDictionary:@{@"hotspot":UNKNOWN_IP_ADDRESS,
                                                                                   @"wireless":UNKNOWN_IP_ADDRESS,
                                                                                   @"wired":UNKNOWN_IP_ADDRESS,
                                                                                   @"cell":UNKNOWN_IP_ADDRESS}];
  
  struct ifaddrs *interfaces = NULL;
  struct ifaddrs *temp_addr = NULL;
  int success = 0;
  // retrieve the current interfaces - returns 0 on success
  success = getifaddrs(&interfaces);
  if (success == 0) {
    // Loop through linked list of interfaces
    temp_addr = interfaces;
    while(temp_addr != NULL) {
      if (temp_addr->ifa_addr == NULL) {
        continue;
      }
      if(temp_addr->ifa_addr->sa_family == AF_INET) {
        // Check if interface is en0 which is the wifi connection on the iPhone
        if([[NSString stringWithUTF8String:temp_addr->ifa_name] isEqualToString:HOTSPOT_IF]) {
          // Get NSString from C String
          [addresses setObject:[NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)] forKey:@"hotspot"];
          
        }
        // Check if interface is en0 which is the wifi connection on the iPhone
        if([[NSString stringWithUTF8String:temp_addr->ifa_name] isEqualToString:WIFI_IF]) {
          // Get NSString from C String
          [addresses setObject:[NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)] forKey:@"wireless"];
          
        }
        // Check if interface is a wired connection
        if([KNOWN_WIRED_IFS containsObject:[NSString stringWithUTF8String:temp_addr->ifa_name]]) {
          [addresses setObject:[NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)] forKey:@"wired"];
        }
        // Check if interface is a cellular connection
        if([KNOWN_CELL_IFS containsObject:[NSString stringWithUTF8String:temp_addr->ifa_name]]) {
          [addresses setObject:[NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)] forKey:@"cell"];
        }
      }
      
      temp_addr = temp_addr->ifa_next;
    }
  }
  // Free memory
  freeifaddrs(interfaces);
  
  return addresses;
}

@end
