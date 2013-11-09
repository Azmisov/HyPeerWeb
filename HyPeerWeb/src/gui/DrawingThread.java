package gui;

import hypeerweb.HyPeerWeb;
import hypeerweb.Node;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Thread for drawing a HyPeerWeb graph
 * @author isaac
 */
public class DrawingThread implements Runnable{
	private final Thread t;
	private final Object lock;
	private final HyPeerWebGraph graph;
	private boolean running = false;
	
	/**
	 * Create a new thread for drawing graphs of nodes in the HyPeerWeb
	 * @param l an instance of the HyPeerWeb segment
	 * @throws Exception if the thread fails to initialize
	 */
	public DrawingThread(HyPeerWeb l) throws Exception{
		lock = l;
		t = new Thread();
		graph = new HyPeerWebGraph(l);
		graph.setVisible(false);
		graph.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				//Unlock the waiting thread
				synchronized (lock){
					lock.notifyAll();
				}
			}
		});
	}
	
	@Override
	public void run(){}
	
	/**
	 * Start drawing a graph of a node
	 * @param n the node to draw
	 */
	public void start(Node n){
		graph.setVisible(true);
		graph.drawNode(n);
		//Start the thread, if it hasn't started already
		if (!running){
			t.start();
			running = true;
		}
	}
}
