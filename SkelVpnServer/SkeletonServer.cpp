#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#include <fcntl.h>

#include <net/if.h>
#include <linux/if_tun.h>
#include <net/if.h>
#include <linux/if_tun.h>

static int get_interface(char *name)
{
    int interface = open("/dev/net/tun", O_RDWR | O_NONBLOCK);
    ifreq ifr;
    memset(&ifr, 0, sizeof(ifr));
    ifr.ifr_flags = IFF_TUN | IFF_NO_PI;
    strncpy(ifr.ifr_name, name, sizeof(ifr.ifr_name));
    if (ioctl(interface, TUNSETIFF, &ifr)) {
        perror("Cannot get TUN interface");
        exit(1);
    }
    printf("Open the tun interface");
    return interface;
}

static int get_tunnel(char *port, char *secret)
{
    printf("We try to create tunnel");
    // We use an IPv6 socket to cover both IPv4 and IPv6.
    int tunnel = socket(AF_INET6, SOCK_DGRAM, 0);
    int flag = 1;
    setsockopt(tunnel, SOL_SOCKET, SO_REUSEADDR, &flag, sizeof(flag));
    flag = 0;
    setsockopt(tunnel, IPPROTO_IPV6, IPV6_V6ONLY, &flag, sizeof(flag));
    // Accept packets received on any local address.
    sockaddr_in6 addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin6_family = AF_INET6;
    addr.sin6_port = htons(atoi(port));
    // Call bind(2) in a loop since Linux does not have SO_REUSEPORT.
    while (bind(tunnel, (sockaddr *)&addr, sizeof(addr))) {
        if (errno != EADDRINUSE) {
            return -1;
        }
        usleep(100000);
    }
    // Receive packets till the secret matches.
    char packet[1024];
    socklen_t addrlen;
    addrlen = sizeof(addr);
    int n = recvfrom(tunnel, packet, sizeof(packet), 0,
            (sockaddr *)&addr, &addrlen);
    if (n <= 0) {
        return -1;
    }
    packet[n] = 0;
    // Connect to the client as we only handle one client at a time.
    connect(tunnel, (sockaddr *)&addr, addrlen);
    printf("Tunnel is established %d", addr);
    return tunnel;
}

int main(int argc, char **argv){
  printf("Hello World");
  printf("Gonna call get_interface\n");
  int interface = get_interface(argv[1]);
  printf("interface = %d\n", interface);

  printf("Gonna call get_tunnel");
  int tunnel = get_tunnel(argv[2], argv[3]);
  printf("tunnel = %d\n", tunnel);

  printf("=== Gonna run loop ===\n");
  while(tunnel != -1) {
    fcntl(tunnel, F_SETFL, O_NONBLOCK);
    // Allocate the buffer for a single packet.
    char packet[32767];
    // We use a timer to determine the status of the tunnel. It
    // works on both sides. A positive value means sending, and
    // any other means receiving. We start with receiving.
    int timer = 0;
    // We keep forwarding packets till something goes wrong.
    while (true) {
        // Assume that we did not make any progress in this iteration.
        bool idle = true;
        // Read the outgoing packet from the input stream.
        int length = read(interface, packet, sizeof(packet));
        if (length > 0) {
            // Write the outgoing packet to the tunnel.
            send(tunnel, packet, length, MSG_NOSIGNAL);
            // There might be more outgoing packets.
            idle = false;
            // If we were receiving, switch to sending.
            if (timer < 1) {
                timer = 1;
            }
        }
        // Read the incoming packet from the tunnel.
        length = recv(tunnel, packet, sizeof(packet), 0);
        if (length == 0) {
            break;
        }
        if (length > 0) {
            // Ignore control messages, which start with zero.
            if (packet[0] != 0) {
                // Write the incoming packet to the output stream.
                write(interface, packet, length);
            }
            // There might be more incoming packets.
            idle = false;
            // If we were sending, switch to receiving.
            if (timer > 0) {
                timer = 0;
            }
        }
        // If we are idle or waiting for the network, sleep for a
        // fraction of time to avoid busy looping.
        if (idle) {
            usleep(100000);
            // Increase the timer. This is inaccurate but good enough,
            // since everything is operated in non-blocking mode.
            timer += (timer > 0) ? 100 : -100;
            // We are receiving for a long time but not sending.
            // Can you figure out why we use a different value? :)
            if (timer < -16000) {
                // Send empty control messages.
                packet[0] = 0;
                for (int i = 0; i < 3; ++i) {
                    send(tunnel, packet, 1, MSG_NOSIGNAL);
                }
                // Switch to sending.
                timer = 1;
            }
            // We are sending for a long time but not receiving.
            if (timer > 20000) {
                break;
            }
        }
    }
    printf("%s: The tunnel is broken\n", argv[1]);
    close(tunnel);
  }


}
