package aqua.blatt1.client;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import aqua.blatt1.common.FishModel;

@SuppressWarnings("serial")
public class TankView extends JPanel implements Observer {
	private final TankModel tankModel;
	private final FishView fishView;
	private final Runnable repaintRunnable;

	public TankView(final TankModel tankModel) {
		this.tankModel = tankModel;
		fishView = new FishView();

		repaintRunnable = new Runnable() {
			@Override
			public void run() {
				repaint();
			}
		};

		setPreferredSize(new Dimension(TankModel.WIDTH, TankModel.HEIGHT));
		setBackground(new Color(175, 200, 235));

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				tankModel.newFish(e.getX(), e.getY());
			}
		});
	}

	@SuppressWarnings("unused")
	private void drawBorders(Graphics2D g2d) {
		g2d.setStroke(new BasicStroke(5));
		g2d.setColor(Color.RED);
		g2d.drawLine(0, 0, 0, TankModel.HEIGHT);
		g2d.drawLine(TankModel.WIDTH - 1, 0, TankModel.WIDTH - 1, TankModel.HEIGHT);
	}

	private void doDrawing(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;

		for (FishModel fishModel : tankModel) {
			g2d.drawImage(fishView.getImage(fishModel), fishModel.getX(), fishModel.getY(), null);
			g2d.drawString(fishModel.getId(), fishModel.getX(), fishModel.getY());
		}

	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		doDrawing(g);
		if (tankModel.hasToken()) {
			Graphics2D g2d = (Graphics2D) g;
			drawBorders(g2d);
		}

		if (tankModel.globalSnapshotDone) {
			Thread guiThread = new Thread(new Runnable() {
				@Override
				public void run() {
					JOptionPane.showMessageDialog(null, tankModel.snapshotToken.getFishies() + " Fishies");
				}
			});
			guiThread.start();
			tankModel.globalSnapshotDone = false;
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		SwingUtilities.invokeLater(repaintRunnable);
	}
}
