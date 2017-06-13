# HyPeerWeb
Implementation of Dr. Scott Woodfield's decentralized "HyPeerWeb" network structure. The graph network is built as
closely as possible to a hypercube, where vertices are computer nodes, and edges are connections between them. As nodes are
added/removed, the hypercube connections are rearranged to maintain the structure.

In this implementation, each HyPeerWeb "segment" can itself be a node in a larger HyPeerWeb, for the cases where you have local
networks within larger ones. There are classes to facilitate caching network nodes, for visualization purposes. There is a chat
server/client demo. Each chat client contains a local HyPeerWeb that you can visualize and debug. Each chat client/server is
connected through a larger HyPeerWeb.

The implementation is mostly experimental, since it has little security features or error-recovery. Features needed for a
production-quality implementation are listed in `src/tasks/Tasks.txt`.

Demo application screenshots:<br>
<img src="https://github.com/Azmisov/HyPeerWeb/raw/master/HyPeerWeb/screenshots/chat.jpg" width="400">
<img src="https://github.com/Azmisov/HyPeerWeb/raw/master/HyPeerWeb/screenshots/graph1.jpg" width="400">
<img src="https://github.com/Azmisov/HyPeerWeb/raw/master/HyPeerWeb/screenshots/graph2.jpg" width="400">
<img src="https://github.com/Azmisov/HyPeerWeb/raw/master/HyPeerWeb/screenshots/graph3.jpg" width="400">
<img src="https://github.com/Azmisov/HyPeerWeb/raw/master/HyPeerWeb/screenshots/list.jpg" width="400">

