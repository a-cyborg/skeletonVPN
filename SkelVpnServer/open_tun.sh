#!/usr/bin/env sh

# Tun interfaces set up for a linux system.
TUN_NAME="tun0"
SRC_ADDR="10.0.0.1/24"
DEST_ADDR="10.0.0.2/24"
TXQUEUELEN="10000"

echo "🐝 Setting up the tun interface \
    | name = $TUN_NAME | src address = $SRC_ADDR | destination address = $DEST_ADDR |"

# Enable Ip forwarding.
echo 1 > /proc/sys/net/ipv4/ip_forward

ip tuntap add name $TUN_NAME mode tun
ip link set $TUN_NAME up
ip link set dev $TUN_NAME txqueuelen $TXQUEUELEN
ip addr add $SRC_ADDR peer $DEST_ADDR dev $TUN_NAME

iptables -t nat -A POSTROUTING -s $DEST_ADDR -j MASQUERADE
iptables -A FORWARD -i $TUN_NAME -s $DEST_ADDR -j ACCEPT
iptables -A FORWARD -o $TUN_NAME -s $DEST_ADDR -j ACCEPT
