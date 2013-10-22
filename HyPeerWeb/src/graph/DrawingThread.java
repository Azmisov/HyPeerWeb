package graph;

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
	private HyPeerWebGraph graph;
	private boolean running = false;
	
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
	
	public void start(Node n, int levels){
		graph.setVisible(true);
		graph.drawNode(n, levels);
		if (!running){
			t.start();
			running = true;
		}
	}
}
