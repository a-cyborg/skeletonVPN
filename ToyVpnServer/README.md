## ToyVpnServer

- Copy of the original [ToyVpnServer](https://android.googlesource.com/platform/development/+/master/samples/ToyVpn/server/linux/ToyVpnServer.cpp) code.
- I copied the server code into this repository to make a minor fix.
    ```c++
    // line no.118 function get_tunnel(port, secret)
    // Receive packets till the secret matches.
    } while (packet[0] != 0 || strcmp(secret, &packet[1]));
    ```
    change to
    ```c++
    } while (packet[0] != 0 && strcmp(secret, &packet[1]));
    ```
- This test server only runs on Linux.

## Run Server
1. Compile the server
```shell
cd ToyVpnServer
g++ -o ToyVpnServer ToyVpnServer.cpp
chmod +x ToyVpnServer 
```

2. Open tun interface  
    open with automation script with default settings:

```shell
sudo ./open_tun.sh
```

Also you can change the default settings:

```shell
vim open_tun.sh
...
TUN_NAME="tun0"
SRC_ADDR="10.0.0.1/32"
DEST_ADDR="10.0.0.2/32"
...
```

or create a tun interface and set the NAT rules manually.

3. Run server

```shell
// # Create a server on port 8000 with shared secret "test".
./ToyVpnServer tun0 8000 test -m 1400 -a 10.0.0.2 32 -d 8.8.8.8 -r 0.0.0.0 0
```
