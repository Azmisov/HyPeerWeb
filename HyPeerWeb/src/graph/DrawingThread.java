package graph;

import hypeerweb.HyPeerWeb;
import hypeerweb.Node;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

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
		graph.addComponentListener(new ComponentListener(){
			@Override
			public void componentHidden(ComponentEvent e) {
				//Unlock the waiting thread
				synchronized (lock){
					lock.notifyAll();
				}
			}
			@Override
			public void componentShown(ComponentEvent e) {}
			@Override
			public void componentResized(ComponentEvent e) {}
			@Override
			public void componentMoved(ComponentEvent e) {}
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
