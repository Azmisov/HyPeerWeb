package chat;

import hypeerweb.HyPeerWeb;
import hypeerweb.Links;
import hypeerweb.Node;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeSet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Draws a directed graph of a HyPeerWeb node
 * <li>Red-links = neighbors</li>
 * <li>Green-links = sneighbors</li>
 * <li>Pink-links = isneighbors</li>
 * <li>Cyan = selected node</li>
 * <li>Yellow = selected node's children</li>
 * <li>Green = selcted node's parent</li>
 * <b>Hotkeys:</b><br/>
 * <li>Right click a node hide it</li>
 * <li>Middle click to restore all hidden nodes</li>
 * @author isaac
 */
public class HyPeerWebGraph extends JFrame{
	private HyPeerWeb web;
	private GraphTab draw;
	private static int winSize = 700;
	private int levels = 2;
	private ActionListener unpause;
	private final JSpinner nodeSpin, levelSpin;
	private boolean deferEvts = false;
	
	/**
	 * Creates a new JFrame that can draw graphs of HyPeerWeb nodes
	 * @param web an instance of the HyPeerWeb segment
	 * @throws Exception if the window fails to initialize
	 */
	public HyPeerWebGraph(final HyPeerWeb web) throws Exception{
		this.web = web;
		//Initialize window
		setTitle("HyPeerWeb Directed Graph");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setSize(winSize, winSize+100);
		setLayout(new BorderLayout(0, 4));
		
		//Toolbar
		JPanel bar = new JPanel();
		nodeSpin = new JSpinner();
		nodeSpin.setModel(new SpinnerNumberModel(2, 0, null, 1));
		nodeSpin.setPreferredSize(new Dimension(45, 25));
		nodeSpin.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				if (!deferEvts){
					Node n = web.getNode((int) nodeSpin.getValue());
					if (n != null) draw.draw(n, 2);
				}
			}
		});
		bar.add(new JLabel("WebID:"));
		bar.add(nodeSpin);
		levelSpin = new JSpinner();
		levelSpin.setPreferredSize(new Dimension(35, 25));
		levelSpin.setModel(new SpinnerNumberModel(2, 0, null, 1));
		levelSpin.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				if (!deferEvts){
					levels = (int) levelSpin.getValue();
					draw.redraw(true);
				}
			}
		});
		bar.add(new JLabel("Levels:"));
		bar.add(levelSpin);
		bar.add(new JLabel("Selected:"));
		JButton hideBtn = new JButton("Hide");
		hideBtn.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				draw.hideNode();
			}
		});
		hideBtn.setToolTipText("Right click");
		JButton viewBtn = new JButton("View");
		viewBtn.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				draw.viewSelected();
			}
		});
		bar.add(hideBtn);
		bar.add(viewBtn);
		bar.add(new JLabel("Other:"));
		JButton unhideBtn = new JButton("Unhide");
		unhideBtn.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				draw.unhideNodes();
			}
		});
		unhideBtn.setToolTipText("Middle click");
		bar.add(unhideBtn);
		
		//Add toolbar and drawing together
		add(bar, BorderLayout.PAGE_START);
		draw = new Graph();
		add(draw, BorderLayout.CENTER);
	}
	
	/**
	 * Draws the specified node
	 * @param n the node to draw
	 */
	public void drawNode(Node n){
		try {
			deferEvts = true;
			levelSpin.setValue(levels);
			nodeSpin.setValue(n.getWebId());
			deferEvts = false;
			draw.draw(n);
		} catch (Exception ex) {
			System.out.println("Failed to draw graph!!!");
		}
	}
}
