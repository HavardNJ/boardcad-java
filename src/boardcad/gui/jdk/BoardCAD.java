package boardcad.gui.jdk;

/*

 * Created on Sep 17, 2005

 *

 * To change the template for this generated file go to

 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments

 */

/**

 * @author H�vard

 *

 * To change the template for this generated type comment go to

 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments

 */

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.File;
import java.util.*;
import java.util.Locale;
import java.util.Timer;
import java.util.prefs.*;

import javax.imageio.ImageIO;
import javax.media.j3d.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.vecmath.*;

import cadcore.*;
import board.*;
import boardcad.settings.*;
import boardcad.gui.jdk.plugin.*;
import boardcad.DefaultBrds;
import boardcad.FileTools;
import boardcad.print.*;
import boardcad.commands.*;
import boardcad.export.DxfExport;
import boardcad.export.GCodeDraw;
import boardcad.export.StlExport;
import boardcad.i18n.LanguageResource;
import boardcam.cutters.AbstractCutter;
import boardcam.MachineConfig;
import boardcam.cutters.SimpleBullnoseCutter;
import boardcam.holdingsystems.SupportsBlankHoldingSystem;
import boardcam.toolpathgenerators.*;
import boardcam.toolpathgenerators.ext.SandwichCompensation;
import boardcam.writers.GCodeWriter;
import board.readers.*;
import board.writers.*;
import boardcad.ScriptLoader;

public class BoardCAD implements Runnable, ActionListener, ItemListener, KeyEventDispatcher {

	protected static BoardCAD mInstance = null;
	private static final String appname = "BoardCAD v3.2 Limited Edition";
	public static String defaultDirectory = "";

	enum DeckOrBottom {
		DECK, BOTTOM, BOTH
	};

	protected BezierBoard mCurrentBrd;
	private BezierBoard mOriginalBrd;
	private BezierBoard mGhostBrd;

	static protected Locale[] mSupportedLanguages = { new Locale("en", ""), new Locale("fr", ""), new Locale("pt", ""),
			new Locale("es", ""), new Locale("no", ""), new Locale("nl", "") };

	private BrdCommand mCurrentCommand;
	private BrdCommand mPreviousCommand;

	private PrintBrd mPrintBrd;
	private PrintSpecSheet mPrintSpecSheet;
	private PrintSandwichTemplates mPrintSandwichTemplates;
	private PrintChamberedWoodTemplate mPrintChamberedWoodTemplate;
	private PrintHollowWoodTemplates mPrintHollowWoodTemplates;

	private boolean mGUIBlocked = true;

	private QuadView mQuadView;

	public QuadView getQuadView() {
		return mQuadView;
	}

	public String getQuadViewActiveName() {
		return getQuadView().getActive().getName();
	}

	private BoardEdit mQuadViewOutlineEdit;
	private BoardEdit mQuadViewCrossSectionEdit;
	private BoardEdit mQuadViewRockerEdit;

	private BoardEdit mOutlineEdit;
	private BoardEdit mCrossSectionEdit;
	private BoardEdit mCrossSectionOutlineEdit;
	private BoardEdit mBottomAndDeckEdit;
	private BoardEdit mOutlineEdit2;

	DeckOrBottom mEditDeckorBottom = DeckOrBottom.DECK;

	private BoardSpec mBoardSpec;

	private JPanel panel;

	private ControlPointInfo mControlPointInfo;

	private JSplitPane mCrossSectionSplitPane;

	private BrdEditSplitPane mOutlineAndProfileSplitPane;

	JPanel mRenderedPanel;
	private JFrame mFrame;

	private JToolBar mToolBar;

	public JSplitPane mSplitPane;
	public JTabbedPane mTabbedPane;
	public JTabbedPane mTabbedPane2;

	private JCheckBoxMenuItem mIsPaintingGridMenuItem;
	private JCheckBoxMenuItem mIsPaintingOriginalBrdMenuItem;
	private JCheckBoxMenuItem mIsPaintingGhostBrdMenuItem;
	private JCheckBoxMenuItem mIsPaintingControlPointsMenuItem;
	private JCheckBoxMenuItem mIsPaintingNonActiveCrossSectionsMenuItem;
	private JCheckBoxMenuItem mIsPaintingGuidePointsMenuItem;
	private JCheckBoxMenuItem mIsPaintingCurvatureMenuItem;
	private JCheckBoxMenuItem mIsPaintingVolumeDistributionMenuItem;
	private JCheckBoxMenuItem mIsPaintingCenterOfMassMenuItem;
	private JCheckBoxMenuItem mIsPaintingSlidingInfoMenuItem;
	private JCheckBoxMenuItem mIsPaintingSlidingCrossSectionMenuItem;
	private JCheckBoxMenuItem mIsPaintingFinsMenuItem;
	private JCheckBoxMenuItem mIsPaintingBackgroundImageMenuItem;
	private JCheckBoxMenuItem mIsPaintingBaseLineMenuItem;
	private JCheckBoxMenuItem mIsPaintingCenterLineMenuItem;
	private JCheckBoxMenuItem mIsPaintingOverCurveMesurementsMenuItem;
	private JCheckBoxMenuItem mIsPaintingMomentOfInertiaMenuItem;
	private JCheckBoxMenuItem mIsPaintingCrossectionsPositionsMenuItem;
	private JCheckBoxMenuItem mIsPaintingFlowlinesMenuItem;
	private JCheckBoxMenuItem mIsPaintingApexlineMenuItem;
	private JCheckBoxMenuItem mIsPaintingTuckUnderLineMenuItem;
	private JCheckBoxMenuItem mIsPaintingFootMarksMenuItem;
	private JCheckBoxMenuItem mIsAntialiasingMenuItem;
	private JCheckBoxMenuItem mUseFillMenuItem;

	private final JMenu mRecentBrdFilesMenu = new JMenu();

	private AbstractAction mSaveBrdAs;
	private AbstractAction mNextCrossSection;
	private AbstractAction mPreviousCrossSection;

	JRadioButtonMenuItem mControlPointInterpolationButton;

	JRadioButtonMenuItem mSBlendInterpolationButton;

	private JCheckBoxMenuItem mShowRenderInwireframe;

	public JToggleButton mLifeSizeButton;

	private boolean mBoardChanged = false;
	protected boolean mGhostMode = false;
	protected boolean mOrgFocus = false;

	public static double mPrintMarginLeft = 72 / 4;
	public static double mPrintMarginRight = 72 / 4;
	public static double mPrintMarginTop = 72 / 4;
	public static double mPrintMarginBottom = 72 / 4;

	protected ThreeDView mRendered3DView;
	protected ThreeDView mQuad3DView;

	public StatusPanel mStatusPanel;

	WeightCalculatorDialog mWeightCalculatorDialog;

	BoardGuidePointsDialog mGuidePointsDialog;

	private JCheckBoxMenuItem mShowBezier3DModelMenuItem;

	private BoardCADSettings mSettings;

	BezierBoardCrossSection mCrossSectionCopy;

	Timer mBezier3DModelUpdateTimer;

	public static void main(final String[] args) {
		BoardCAD.getInstance();
	}

	public static BoardCAD getInstance() {
		if (mInstance == null) {
			mInstance = new BoardCAD();
			mInstance.init();
		}
		return mInstance;
	}

	protected BoardCAD() {
		/*
		 * // Test gcodedraw GeneralPath squarePath = new GeneralPath();
		 * squarePath.moveTo(1.0, -1.0); squarePath.lineTo(1.0, 1.0);
		 * squarePath.lineTo(-1.0, 1.0); squarePath.lineTo(-1.0, -1.0);
		 * squarePath.closePath();
		 *
		 * GeneralPath linePath = new GeneralPath(); linePath.moveTo(-1.0, 0.0);
		 * int steps = 100; for (int i = 0; i < steps; i++) {
		 * linePath.moveTo(-1.0 + ((2.0 * i) / steps), 0.0); }
		 *
		 * GCodeDraw gdrawSquare = new GCodeDraw(
		 * "C:/Users/Haavard/Desktop/G-Code/SquareTest.nc", 0.05, -0.05, 0.05,
		 * 0.01, 0.2, 0.03); GCodeDraw gdrawSquareNoOffset = new GCodeDraw(
		 * "C:/Users/Haavard/Desktop/G-Code/SquareTestNoOffset.nc", 0.0, -0.05,
		 * 0.05, 0.01, 0.2, 0.03);
		 *
		 * gdrawSquare.draw(squarePath); gdrawSquareNoOffset.draw(squarePath);
		 */
	}

	protected void init() {
		LanguageResource.init(this);

		mCurrentBrd = new BezierBoard();
		mGhostBrd = new BezierBoard();
		mOriginalBrd = new BezierBoard();

		mSettings = BoardCADSettings.getInstance();

		mRecentBrdFilesMenu.setText(LanguageResource.getString("RECENTFILES_STR"));

		// Run the application
		SwingUtilities.invokeLater(this);
	}

	public void getPreferences() {
		// Preference keys for this package

		final Preferences prefs = Preferences.userNodeForPackage(BoardCAD.class);

		defaultDirectory = prefs.get("defaultDirectory", "");
		mIsPaintingGridMenuItem
				.setSelected(prefs.getBoolean("mIsPaintingGridMenuItem", mIsPaintingGridMenuItem.isSelected()));
		mIsPaintingOriginalBrdMenuItem.setSelected(
				prefs.getBoolean("mIsPaintingOriginalBrdMenuItem", mIsPaintingOriginalBrdMenuItem.isSelected()));
		mIsPaintingGhostBrdMenuItem
				.setSelected(prefs.getBoolean("mIsPaintingGhostBrdMenuItem", mIsPaintingGhostBrdMenuItem.isSelected()));
		mIsPaintingControlPointsMenuItem.setSelected(
				prefs.getBoolean("mIsPaintingControlPointsMenuItem", mIsPaintingControlPointsMenuItem.isSelected()));
		mIsPaintingNonActiveCrossSectionsMenuItem.setSelected(prefs.getBoolean(
				"mIsPaintingNonActiveCrossSectionsMenuItem", mIsPaintingNonActiveCrossSectionsMenuItem.isSelected()));
		mIsPaintingGuidePointsMenuItem.setSelected(
				prefs.getBoolean("mIsPaintingGuidePointsMenuItem", mIsPaintingGuidePointsMenuItem.isSelected()));
		mIsPaintingCurvatureMenuItem.setSelected(
				prefs.getBoolean("mIsPaintingCurvatureMenuItem", mIsPaintingCurvatureMenuItem.isSelected()));
		mIsPaintingVolumeDistributionMenuItem.setSelected(prefs.getBoolean("mIsPaintingVolumeDistributionMenuItem",
				mIsPaintingVolumeDistributionMenuItem.isSelected()));
		mIsPaintingCenterOfMassMenuItem.setSelected(
				prefs.getBoolean("mIsPaintingCenterOfMassMenuItem", mIsPaintingCenterOfMassMenuItem.isSelected()));
		mIsPaintingSlidingInfoMenuItem.setSelected(
				prefs.getBoolean("mIsPaintingSlidingInfoMenuItem", mIsPaintingSlidingInfoMenuItem.isSelected()));
		mIsPaintingSlidingCrossSectionMenuItem.setSelected(prefs.getBoolean("mIsPaintingSlidingCrossSectionMenuItem",
				mIsPaintingSlidingCrossSectionMenuItem.isSelected()));
		mIsPaintingFinsMenuItem
				.setSelected(prefs.getBoolean("mIsPaintingFinsMenuItem", mIsPaintingFinsMenuItem.isSelected()));
		mIsPaintingBackgroundImageMenuItem.setSelected(prefs.getBoolean("mIsPaintingBackgroundImageMenuItem",
				mIsPaintingBackgroundImageMenuItem.isSelected()));
		mIsAntialiasingMenuItem
				.setSelected(prefs.getBoolean("mIsAntialiasingMenuItem", mIsAntialiasingMenuItem.isSelected()));
		mUseFillMenuItem.setSelected(prefs.getBoolean("mUseFillMenuItem", mUseFillMenuItem.isSelected()));
		mIsPaintingBaseLineMenuItem
				.setSelected(prefs.getBoolean("mIsPaintingBaseLineMenuItem", mIsPaintingBaseLineMenuItem.isSelected()));
		mIsPaintingCenterLineMenuItem.setSelected(
				prefs.getBoolean("mIsPaintingCenterLineMenuItem", mIsPaintingCenterLineMenuItem.isSelected()));
		mIsPaintingOverCurveMesurementsMenuItem.setSelected(prefs.getBoolean("mIsPaintingoverCurveMesurementsMenuItem",
				mIsPaintingOverCurveMesurementsMenuItem.isSelected()));
		mIsPaintingMomentOfInertiaMenuItem.setSelected(prefs.getBoolean("mIsPaintingMomentOfInertiaMenuItem",
				mIsPaintingMomentOfInertiaMenuItem.isSelected()));

		mIsPaintingCrossectionsPositionsMenuItem.setSelected(prefs.getBoolean(
				"mIsPaintingCrossectionsPositionsMenuItem", mIsPaintingCrossectionsPositionsMenuItem.isSelected()));

		mIsPaintingFlowlinesMenuItem.setSelected(
				prefs.getBoolean("mIsPaintingFlowlinesMenuItem", mIsPaintingFlowlinesMenuItem.isSelected()));

		mIsPaintingApexlineMenuItem
				.setSelected(prefs.getBoolean("mIsPaintingApexlineMenuItem", mIsPaintingApexlineMenuItem.isSelected()));

		mIsPaintingTuckUnderLineMenuItem.setSelected(
				prefs.getBoolean("mIsPaintingTuckUnderLineMenuItem", mIsPaintingTuckUnderLineMenuItem.isSelected()));
		mIsPaintingFootMarksMenuItem.setSelected(
				prefs.getBoolean("mIsPaintingFootMarksMenuItem", mIsPaintingFootMarksMenuItem.isSelected()));

		mPrintMarginLeft = prefs.getDouble("mPrintMarginLeft", mPrintMarginLeft);
		mPrintMarginRight = prefs.getDouble("mPrintMarginRight", mPrintMarginRight);
		mPrintMarginTop = prefs.getDouble("mPrintMarginTop", mPrintMarginTop);
		mPrintMarginBottom = prefs.getDouble("mPrintMarginBottom", mPrintMarginBottom);

		final int type = prefs.getInt("CrossSectionInterpolationType", getCrossSectionInterpolationTypeAsInt());
		setCrossSectionInterpolationTypeFromInt(type);

		for (int i = 8; i >= 0; i--) {
			String id = "mRecentBrdFiles" + i;
			String string = prefs.get(id, "");
			if (string == null || string.compareTo("") == 0)
				continue;

			addRecentBoardFile(string);
		}

		mSettings.getPreferences();
	}

	public void putPreferences() {
		// Preference keys for this package
		final Preferences prefs = Preferences.userNodeForPackage(BoardCAD.class);

		prefs.put("defaultDirectory", defaultDirectory);
		prefs.putBoolean("mIsPaintingGridMenuItem", mIsPaintingGridMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingOriginalBrdMenuItem", mIsPaintingOriginalBrdMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingGhostBrdMenuItem", mIsPaintingGhostBrdMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingControlPointsMenuItem", mIsPaintingControlPointsMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingNonActiveCrossSectionsMenuItem",
				mIsPaintingNonActiveCrossSectionsMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingGuidePointsMenuItem", mIsPaintingGuidePointsMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingCurvatureMenuItem", mIsPaintingCurvatureMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingVolumeDistributionMenuItem", mIsPaintingVolumeDistributionMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingCenterOfMassMenuItem", mIsPaintingCenterOfMassMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingSlidingInfoMenuItem", mIsPaintingSlidingInfoMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingSlidingCrossSectionMenuItem", mIsPaintingSlidingCrossSectionMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingFinsMenuItem", mIsPaintingFinsMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingBackgroundImageMenuItem", mIsPaintingCrossectionsPositionsMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingBaseLineMenuItem", mIsPaintingBaseLineMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingCenterLineMenuItem", mIsPaintingCenterLineMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingOverCurveMesurementsMenuItem",
				mIsPaintingOverCurveMesurementsMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingMomentOfInertiaMenuItem", mIsPaintingMomentOfInertiaMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingCrossectionsPositionsMenuItem",
				mIsPaintingCrossectionsPositionsMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingFlowlinesMenuItem", mIsPaintingFlowlinesMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingApexlineMenuItem", mIsPaintingApexlineMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingTuckUnderLineMenuItem", mIsPaintingTuckUnderLineMenuItem.isSelected());
		prefs.putBoolean("mIsPaintingFootMarksMenuItem", mIsPaintingFootMarksMenuItem.isSelected());
		prefs.putBoolean("mIsAntialiasingMenuItem", mIsAntialiasingMenuItem.isSelected());
		prefs.putBoolean("mUseFillMenuItem", mUseFillMenuItem.isSelected());
		prefs.putInt("CrossSectionInterpolationType", getCrossSectionInterpolationTypeAsInt());

		prefs.putDouble("mPrintMarginLeft", mPrintMarginLeft);
		prefs.putDouble("mPrintMarginRight", mPrintMarginRight);
		prefs.putDouble("mPrintMarginTop", mPrintMarginTop);
		prefs.putDouble("mPrintMarginBottom", mPrintMarginBottom);

		for (int i = 0; i < mRecentBrdFilesMenu.getMenuComponentCount(); i++) {
			String str = ((JMenuItem) mRecentBrdFilesMenu.getMenuComponent(i)).getText();
			String id = "mRecentBrdFiles" + i;
			prefs.put(id, str);
		}

		mSettings.putPreferences();
	}

	void addRecentBoardFile(final String filename) {
		// Remove item if already exists
		for (int i = 0; i < mRecentBrdFilesMenu.getMenuComponentCount(); i++) {
			JMenuItem menuItem = (JMenuItem) mRecentBrdFilesMenu.getMenuComponent(i);
			String str = menuItem.getText();

			if (str.compareTo(filename) == 0) {
				mRecentBrdFilesMenu.remove(menuItem);
				break;
			}
		}

		final BoardLoadAction loadRecentBrd = new BoardLoadAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, filename);
				mBrd = BoardCAD.getInstance().getCurrentBrd();
				mCloneBrd = BoardCAD.getInstance().getOriginalBrd();
			};

			@Override
			public void actionPerformed(ActionEvent event) {
				int r = saveChangedBoard();
				if (r == -1 || r == 2) // closed dialog or cancel button pressed
					return;

				String filename = (String) this.getValue(Action.NAME);

				super.load(filename);

				addRecentBoardFile(filename);

				fitAll();
				onBrdChanged();
				onControlPointChanged();
				mBoardChanged = false;
				boolean selected = mShowBezier3DModelMenuItem.getModel().isSelected();
				if (selected) {
					updateBezier3DModel();
				}
				redraw();
			}
		};

		mRecentBrdFilesMenu.add(new JMenuItem(loadRecentBrd), 0);

		while (mRecentBrdFilesMenu.getMenuComponentCount() > 8) {
			mRecentBrdFilesMenu.remove(mRecentBrdFilesMenu.getMenuComponentCount() - 1);
		}
	}

	public void updateBezier3DModel() {
		if (mTabbedPane.getSelectedComponent() == mRenderedPanel) {
			mRendered3DView.updateBezier3DModel(getCurrentBrd());
		} else if (mTabbedPane.getSelectedComponent() == mQuadView) {
			mQuad3DView.updateBezier3DModel(getCurrentBrd());
		}
	}

	public void setCurrentCommand(final BrdCommand command) {
		mCurrentCommand = command;
	}

	public BrdCommand getCurrentCommand() {
		return mCurrentCommand;
	}

	public void setSelectedEdit(final Component edit) {
		if (edit == mCrossSectionEdit) {
			mTabbedPane.setSelectedComponent(mCrossSectionSplitPane);
		} else if (edit == mOutlineEdit2) {
			mTabbedPane.setSelectedComponent(mOutlineAndProfileSplitPane);
		} else if (edit == mBottomAndDeckEdit) {
			mTabbedPane.setSelectedComponent(mBottomAndDeckEdit);
		} else if (edit == mQuadViewOutlineEdit || edit == mQuadViewCrossSectionEdit || edit == mQuadViewRockerEdit) {
			mTabbedPane.setSelectedComponent(mQuadView);
		} else {
			mTabbedPane.setSelectedComponent(edit);
		}
	}

	public JTabbedPane getTabbedPane() {
		return mTabbedPane;
	}

	public BoardEdit getSelectedEdit() {
		try {
			final Component component = mTabbedPane.getSelectedComponent();

			if (component == mCrossSectionSplitPane) {
				return mCrossSectionEdit;
			} else if (component == mOutlineAndProfileSplitPane) {
				return mOutlineAndProfileSplitPane.getActive();
			} else if (component == mQuadView) {
				return mQuadView.getActive();
			} else if (component instanceof BoardEdit) {
				return (BoardEdit) component;
			} else {
				return null;
			}
		} catch (final Exception e) {
			System.out.println("BoardCAD.getSelectedEdit() Exception: " + e.toString());
			return null;
		}
	}

	public BoardGuidePointsDialog getGuidePointsDialog() {
		return mGuidePointsDialog;
	}

	public JFrame getFrame() {
		return mFrame;
	}

	// public MachineView getMachineView() {
	// return mMachineView;
	// }

	public ControlPointInfo getControlPointInfo() {
		return mControlPointInfo;
	}

	public boolean isPaintingOriginalBrd() {
		return mIsPaintingOriginalBrdMenuItem.isSelected();
	}

	public boolean isPaintingGhostBrd() {
		return mIsPaintingGhostBrdMenuItem.isSelected();
	}

	public boolean isPaintingGrid() {
		return mIsPaintingGridMenuItem.isSelected();
	}

	public boolean isPaintingControlPoints() {
		return mIsPaintingControlPointsMenuItem.isSelected();
	}

	public boolean isPaintingNonActiveCrossSections() {
		return mIsPaintingNonActiveCrossSectionsMenuItem.isSelected();
	}

	public boolean isPaintingGuidePoints() {
		return mIsPaintingGuidePointsMenuItem.isSelected();
	}

	public boolean isPaintingCurvature() {
		return mIsPaintingCurvatureMenuItem.isSelected();
	}

	public boolean isPaintingVolumeDistribution() {
		return mIsPaintingVolumeDistributionMenuItem.isSelected();
	}

	public boolean isPaintingCenterOfMass() {
		return mIsPaintingCenterOfMassMenuItem.isSelected();
	}

	public boolean isPaintingSlidingInfo() {
		return mIsPaintingSlidingInfoMenuItem.isSelected();
	}

	public boolean isPaintingSlidingCrossSection() {
		return mIsPaintingSlidingCrossSectionMenuItem.isSelected();
	}

	public boolean isPaintingFins() {
		return mIsPaintingFinsMenuItem.isSelected();
	}

	public boolean isPaintingBackgroundImage() {
		return mIsPaintingBackgroundImageMenuItem.isSelected();
	}

	public boolean isAntialiasing() {
		return mIsAntialiasingMenuItem.isSelected();
	}

	public boolean isPaintingBaseLine() {
		return mIsPaintingBaseLineMenuItem.isSelected();
	}

	public boolean isPaintingCenterLine() {
		return mIsPaintingCenterLineMenuItem.isSelected();
	}

	public boolean isPaintingOverCurveMeasurements() {
		return mIsPaintingOverCurveMesurementsMenuItem.isSelected();
	}

	public boolean isPaintingMomentOfInertia() {
		return mIsPaintingMomentOfInertiaMenuItem.isSelected();
	}

	public boolean isPaintingCrossectionsPositions() {
		return mIsPaintingCrossectionsPositionsMenuItem.isSelected();
	}

	public boolean isPaintingFlowlines() {
		return mIsPaintingFlowlinesMenuItem.isSelected();
	}

	public boolean isPaintingApexline() {
		return mIsPaintingApexlineMenuItem.isSelected();
	}

	public boolean isPaintingTuckUnderLine() {
		return mIsPaintingTuckUnderLineMenuItem.isSelected();
	}

	public boolean isPaintingFootMarks() {
		return mIsPaintingFootMarksMenuItem.isSelected();
	}

	public boolean useFill() {
		return mUseFillMenuItem.isSelected();
	}

	public boolean isGhostMode() {
		return mGhostMode;
	}

	public boolean isOrgFocus() {
		return mOrgFocus;
	}

	public boolean isGUIBlocked() {
		return mGUIBlocked;
	}

	public AbstractBezierBoardSurfaceModel.ModelType getCrossSectionInterpolationType() {
		if (mControlPointInterpolationButton == null)
			return AbstractBezierBoardSurfaceModel.ModelType.SLinearInterpolation;

		if (mSBlendInterpolationButton.isSelected())
			return AbstractBezierBoardSurfaceModel.ModelType.SLinearInterpolation;
		else
			return AbstractBezierBoardSurfaceModel.ModelType.ControlPointInterpolation;
	}

	public int getCrossSectionInterpolationTypeAsInt() {
		AbstractBezierBoardSurfaceModel.ModelType type = getCrossSectionInterpolationType();
		switch (type) {
		default:
		case ControlPointInterpolation:
			return 2;
		case SLinearInterpolation:
			return 3;
		}
	}

	public void setCrossSectionInterpolationType(final AbstractBezierBoardSurfaceModel.ModelType type) {
		switch (type) {
		default:
		case ControlPointInterpolation:
			mControlPointInterpolationButton.doClick();
			break;
		case SLinearInterpolation:
			mSBlendInterpolationButton.doClick();
			break;
		}
	}

	public void setCrossSectionInterpolationTypeFromInt(int type) {
		switch (type) {
		default:
		case 2:
			mControlPointInterpolationButton.doClick();
			break;
		case 3:
			mSBlendInterpolationButton.doClick();
			break;
		}
		if (mCurrentBrd != null) {
			mCurrentBrd.setInterpolationType(getCrossSectionInterpolationType());
		}
	}

	public BezierBoard getCurrentBrd() {
		return mCurrentBrd;
	}

	public BezierBoard getOriginalBrd() {
		return mOriginalBrd;
	}

	public BezierBoard getGhostBrd() {
		return mGhostBrd;
	}

	public void redraw() {
		mOutlineEdit.repaint();
		mBottomAndDeckEdit.repaint();
		mQuadViewOutlineEdit.repaint();
		mQuadViewCrossSectionEdit.repaint();
		mQuadViewRockerEdit.repaint();
	}

	BezierBoard getFocusedBoard() {
		if (isGhostMode()) {
			return BoardCAD.getInstance().getGhostBrd();
		} else if (mOrgFocus) {
			return BoardCAD.getInstance().getOriginalBrd();
		} else {
			return BoardCAD.getInstance().getCurrentBrd();
		}

	}

	public void fitAll() {
		mOutlineEdit.fitAll();
		// mOutlineEdit2.fit_all();
		mBottomAndDeckEdit.fitAll();
		mCrossSectionEdit.fitAll();

		mQuadViewOutlineEdit.fitAll();
		mQuadViewCrossSectionEdit.fitAll();
		mQuadViewRockerEdit.fitAll();

		// mMachineView.fit_all();

	}

	public void onBrdChanged() {
		updateScreenValues();

		mBoardChanged = true;
		setBoardChangedFor3D();

		if (mBezier3DModelUpdateTimer != null) {
			mBezier3DModelUpdateTimer.cancel();
			mBezier3DModelUpdateTimer = null;
		}

		mBezier3DModelUpdateTimer = new Timer("Bezier3DModelUpdateTimer");
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				mBezier3DModelUpdateTimer = null;
				updateBezier3DModel();
			}
		};
		mBezier3DModelUpdateTimer.schedule(task, 300);
	}

	public void updateScreenValues() {
		if (getCurrentBrd().isEmpty()) {
			return;
		}

		final double length = getCurrentBrd().getLength();

		final double maxWidth = getCurrentBrd().getMaxWidth();

		mFrame.setTitle(appname + " - " + getCurrentBrd().getFilename() + "  "
				+ UnitUtils.convertLengthToCurrentUnit(length, true) + " x "
				+ UnitUtils.convertLengthToCurrentUnit(maxWidth, false));

		mBoardSpec.updateInfo();

		if (mWeightCalculatorDialog.isVisible())
			mWeightCalculatorDialog.updateAll();

		if (mGuidePointsDialog.isVisible())
			mGuidePointsDialog.update();

	}

	void setBoardChangedFor3D() {
		mRendered3DView.setBoardChangedFor3D();
		mQuad3DView.setBoardChangedFor3D();
	}

	protected void setCurrentUnit(int unitType) {
		UnitUtils.setCurrentUnit(unitType);
		if (mWeightCalculatorDialog != null)
			mWeightCalculatorDialog.updateAll();
		if (mGuidePointsDialog != null)
			mGuidePointsDialog.update();
		updateScreenValues();
		onControlPointChanged();
		redraw();
	}

	public void onControlPointChanged() {
		final String className = getCurrentCommand().getClass().getSimpleName();

		if (className.compareTo("BrdEditCommand") == 0) {
			final BoardEdit edit = getSelectedEdit();

			if ((edit != null) && edit.getSelectedControlPoints().size() == 1) {
				final BrdEditCommand cmd = (BrdEditCommand) getCurrentCommand();
				mControlPointInfo.mCmd = cmd;
				mControlPointInfo.setEnabled(true);
				final ArrayList<BezierKnot> controlPoints = edit.getSelectedControlPoints();
				final BezierKnot controlPoint = controlPoints.get(0);
				mControlPointInfo.setControlPoint(controlPoint);
				mControlPointInfo.setWhich(cmd.getWhich());
			} else {
				mControlPointInfo.setEnabled(false);
			}
		}
	}

	public void onSettingsChanged(Setting setting) {
		if (mControlPointInfo != null) {
			mControlPointInfo.setColors();
			mFrame.repaint();
		}

		if (setting.key() == BoardCADSettings.RENDERBACKGROUNDCOLOR) {
			if (mRendered3DView != null)
				mRendered3DView.setBackgroundColor(mSettings.getRenderBackgroundColor());
			if (mQuad3DView != null)
				mQuad3DView.setBackgroundColor(mSettings.getRenderBackgroundColor());
		}
	}

	private void saveAs(String filename) {

		final String ext = FileTools.getExtension(filename);
		BrdWriter.saveFile(getCurrentBrd(), filename);

		addRecentBoardFile(getCurrentBrd().getFilename());

		onBrdChanged();
		mBoardChanged = false;
	}

	private int saveChangedBoard() {
		if (mBoardChanged == true) {
			final Object[] options = { LanguageResource.getString("YESBUTTON_STR"),
					LanguageResource.getString("NOBUTTON_STR"), LanguageResource.getString("CANCELBUTTON_STR") };
			final int n = JOptionPane.showOptionDialog(mFrame, LanguageResource.getString("SAVECURRENTBOARDMSG_STR"),
					LanguageResource.getString("SAVECURRENTBOARDTITLE_STR"), JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

			switch (n) {
			case 0:
				mSaveBrdAs.actionPerformed(null);
				return n;
			case -1:
				return n; // break out by close button
			case 1:
				return n;
			case 2:
				return n; // break out
			default:
				return n;

			}
		}
		return 0;
	}

	/**
	 *
	 * Creates and shows the GUI. This method should be
	 *
	 * invoked on the event-dispatching thread.
	 *
	 */

	@Override
	public void run() {
		createAndShowGUI();
	}

	/**
	 *
	 * Brings up a window that contains a ClickMe component.
	 *
	 * For thread safety, this method should be invoked from
	 *
	 * the event-dispatching thread.
	 *
	 */

	private void createAndShowGUI() {

		// Make sure we have nice window decorations.
		JFrame.setDefaultLookAndFeelDecorated(false);

		// Create and set up the window.
		mFrame = new JFrame(" " + appname);
		mFrame.setMinimumSize(new Dimension(1000, 700));

		mFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		mFrame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				int r = saveChangedBoard();
				if (r == -1 || r == 2) // closed dialog or cancel button pressed
					return;
				putPreferences();
				System.exit(1);
			}
		});

		// Set up the layout manager.
		mFrame.getContentPane().setLayout(new BorderLayout());

		// Insert Icon on JFrame
		try {
			ImageIcon icon = new ImageIcon(getClass().getResource("../../icons/BoardCAD icon 32x32.png"));
			mFrame.setIconImage(icon.getImage());
		} catch (Exception e) {
			System.out.println("Jframe Icon error:\n" + e.getMessage());
		}

		JMenuBar menuBar;
		JPopupMenu popupMenu;

		// Menu
		menuBar = new JMenuBar();
		popupMenu = new JPopupMenu();
		final JMenu fileMenu = new JMenu(LanguageResource.getString("FILEMENU_STR"));
		fileMenu.setMnemonic(KeyEvent.VK_F);
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		final AbstractAction newBrd = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("NEWBOARD_STR"));
				this.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("NEWBOARD_STR"));
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));

			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				String str = (String) JOptionPane.showInputDialog(mFrame, LanguageResource.getString("NEWBOARDMSG_STR"),
						LanguageResource.getString("NEWBOARDTITLE_STR"), JOptionPane.PLAIN_MESSAGE, null,
						DefaultBrds.getInstance().getDefaultBoardsList(),
						DefaultBrds.getInstance().getDefaultBoardsList()[0]);

				if (str == null)
					return;

				BrdReader.loadFile(getCurrentBrd(), DefaultBrds.getInstance().getBoardArray(str), str);
				mOriginalBrd.set(getCurrentBrd());
				fitAll();
				onBrdChanged();
				onControlPointChanged();

				BrdCommandHistory.getInstance().clear();
				mFrame.repaint();
				mBoardChanged = false;
				boolean selected = mShowBezier3DModelMenuItem.getModel().isSelected();
				if (selected) {
					updateBezier3DModel();
				}
			}

		};
		fileMenu.add(newBrd);

		final BoardLoadAction loadBrd = new BoardLoadAction() {
			static final long serialVersionUID = 1L;
			{
				mBrd = mCurrentBrd;
				mCloneBrd = mOriginalBrd;
			};

			@Override
			public void actionPerformed(ActionEvent event) {

				int r = saveChangedBoard();
				if (r == -1 || r == 2) // closed dialog or cancel button pressed
					return;

				super.actionPerformed(event);

				addRecentBoardFile(getCurrentBrd().getFilename());

				fitAll();
				onBrdChanged();
				onControlPointChanged();
				mBoardChanged = false;
				boolean selected = mShowBezier3DModelMenuItem.getModel().isSelected();
				if (selected) {
					updateBezier3DModel();
				}
				redraw();

			}
		};
		loadBrd.putValue(Action.NAME, LanguageResource.getString("BOARDOPEN_STR"));
		loadBrd.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("BOARDOPEN_STR"));
		loadBrd.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		fileMenu.add(loadBrd);

		fileMenu.add(mRecentBrdFilesMenu);
		fileMenu.addSeparator();

		final AbstractAction saveBrd = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("BOARDSAVE_STR"));
				this.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("BOARDSAVE_STR"));
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				saveAs(getCurrentBrd().getFilename());
				// BrdWriter.saveFile(getCurrentBrd(),
				// getCurrentBrd().getFilename());

				mBoardChanged = false;
			}

		};
		fileMenu.add(saveBrd);

		mSaveBrdAs = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("BOARDSAVEAS_STR"));
				this.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/save-as.png")));
			};

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));
				fc.setSelectedFile(new File(getCurrentBrd().getFilename()));

				final int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				final File file = fc.getSelectedFile();

				final String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				BoardCAD.defaultDirectory = file.getPath();

				saveAs(filename);

			}

		};
		fileMenu.add(mSaveBrdAs);

		final AbstractAction SaveBrd = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("BOARDSAVEANDREFRESH_STR"));
				this.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("BOARDSAVEANDREFRESH_STR"));
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				BrdWriter.saveFile(getCurrentBrd(), getCurrentBrd().getFilename());

				mOriginalBrd = (BezierBoard) getCurrentBrd().clone();

				mBoardChanged = false;
			}

		};
		fileMenu.add(SaveBrd);
		fileMenu.addSeparator();

		final BoardLoadAction loadGhost = new BoardLoadAction(mGhostBrd) {
			@Override
			public void actionPerformed(ActionEvent event) {
				super.actionPerformed(event);
				mIsPaintingGhostBrdMenuItem.setSelected(true);
				getSelectedEdit().repaint();
			}
		};
		loadGhost.putValue(Action.NAME, LanguageResource.getString("OPENGHOSTBOARD_STR"));
		loadGhost.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
		fileMenu.add(loadGhost);

		final AbstractAction loadImage = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("LOADBACKGROUNDIMAGE_STR"));
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				final JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showOpenDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				BoardCAD.defaultDirectory = file.getPath();

				BoardEdit edit = getSelectedEdit();
				if (edit == null)
					return;

				edit.loadBackgroundImage(filename);
				mIsPaintingBackgroundImageMenuItem.setSelected(true);
				edit.repaint();
			}

		};
		fileMenu.add(loadImage);
		fileMenu.addSeparator();

		final JMenu printMenu = new JMenu(LanguageResource.getString("PRINTMENU_STR"));
		final JMenuItem printOutline = new JMenuItem(LanguageResource.getString("PRINTOUTLINE_STR"), KeyEvent.VK_O);
		printOutline.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));
		printOutline.addActionListener(this);
		printMenu.add(printOutline);

		final JMenuItem printSpinTemplate = new JMenuItem(LanguageResource.getString("PRINTSPINTEMPLATE_STR"),
				KeyEvent.VK_T);
		// printOutline.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2,
		// ActionEvent.ALT_MASK));
		printSpinTemplate.addActionListener(this);
		printMenu.add(printSpinTemplate);

		final JMenuItem printProfile = new JMenuItem(LanguageResource.getString("PRINTPROFILE_STR"), KeyEvent.VK_P);
		printProfile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.ALT_MASK));
		printProfile.addActionListener(this);
		printMenu.add(printProfile);

		final JMenuItem printSlices = new JMenuItem(LanguageResource.getString("PRINTCROSSECTION_STR"), KeyEvent.VK_S);
		printSlices.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, ActionEvent.ALT_MASK));
		printSlices.addActionListener(this);
		printMenu.add(printSlices);

		printMenu.addSeparator();

		final JMenu printSandwichMenu = new JMenu(LanguageResource.getString("PRINTSANDWICHTEMPLATESMENU_STR"));

		final AbstractAction printProfileTemplate = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTSANDWICHPROFILETEMPLATE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("SANDWICHPARAMETERSCATEGORY_STR");
				Settings sandwichSettings = settings.addCategory(categoryName);
				sandwichSettings.addMeasurement("SkinThickness", 0.3,
						LanguageResource.getString("SANDWICHSKINTHICKNESS_STR"));
				sandwichSettings.addBoolean("Flatten", false, LanguageResource.getString("SANDWICHFLATTEN_STR"));
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("PRINTSANDWICHPROFILETEMPLATETITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}

				mPrintSandwichTemplates.printProfileTemplate(sandwichSettings.getMeasurement("SkinThickness"),
						sandwichSettings.getBoolean("Flatten"), 0.0);
				settingsDialog.dispose();
			}

		};
		printSandwichMenu.add(printProfileTemplate);

		final AbstractAction printRailTemplate = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTSANDWICHRAILTEMPLATE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("SANDWICHPARAMETERSCATEGORY_STR");
				Settings sandwichSettings = settings.addCategory(categoryName);
				sandwichSettings.addMeasurement("SkinThickness", 0.3,
						LanguageResource.getString("SANDWICHSKINTHICKNESS_STR"));
				sandwichSettings.addMeasurement("ToRail", 2.54 / 2,
						LanguageResource.getString("SANDWICHDISTANCETORAIL_STR"));
				sandwichSettings.addMeasurement("TailOffset", 2.0, LanguageResource.getString("SANDWICHTAILOFFSET"));
				sandwichSettings.addMeasurement("NoseOffset", 6.0, LanguageResource.getString("SANDWICHNOSEOFFSET"));
				sandwichSettings.addBoolean("Flatten", false, LanguageResource.getString("SANDWICHFLATTEN_STR"));
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("PRINTSANDWICHRAILTEMPLATETITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}

				mPrintSandwichTemplates.printRailTemplate(sandwichSettings.getMeasurement("ToRail"),
						sandwichSettings.getMeasurement("SkinThickness"), sandwichSettings.getMeasurement("TailOffset"),
						sandwichSettings.getMeasurement("NoseOffset"), sandwichSettings.getBoolean("Flatten"));
				settingsDialog.dispose();
			}

		};
		printSandwichMenu.add(printRailTemplate);

		final AbstractAction printDeckSkin = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTSANDWICHDECKSKINTEMPLATE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("SANDWICHPARAMETERSCATEGORY_STR");
				Settings sandwichSettings = settings.addCategory(categoryName);
				sandwichSettings.addMeasurement("ToRail", 2.54 / 2,
						LanguageResource.getString("SANDWICHDISTANCETORAIL_STR"));
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("PRINTSANDWICHDECKSKINTEMPLATETITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}

				mPrintSandwichTemplates.printDeckSkinTemplate(sandwichSettings.getMeasurement("ToRail"));
				settingsDialog.dispose();
			}

		};
		printSandwichMenu.add(printDeckSkin);

		final AbstractAction printBottomSkin = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTSANDWICHBOTTOMSKINTEMPLATE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("SANDWICHPARAMETERSCATEGORY_STR");
				Settings sandwichSettings = settings.addCategory(categoryName);
				sandwichSettings.addMeasurement("ToRail", 2.54 / 2,
						LanguageResource.getString("SANDWICHDISTANCETORAIL_STR"));
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("PRINTSANDWICHBOTTOMSKINTEMPLATETITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}

				mPrintSandwichTemplates.printDeckSkinTemplate(sandwichSettings.getMeasurement("ToRail"));
				settingsDialog.dispose();
			}

		};
		printSandwichMenu.add(printBottomSkin);

		printMenu.add(printSandwichMenu);

		printMenu.addSeparator();

		final JMenu printHWSMenu = new JMenu(LanguageResource.getString("PRINTHWSMENU_STR"));

		final AbstractAction printHWSSTringer = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTHWSSTRINGER_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("SkinThickness", 0.4, LanguageResource.getString("HWSSKINTHICKNESS_STR"));
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				settings.getPreferences();
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("PRINTHWSSTRINGERTITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				mPrintHollowWoodTemplates.printStringerTemplate(HWSSettings.getMeasurement("SkinThickness"),
						HWSSettings.getMeasurement("FrameThickness"), HWSSettings.getMeasurement("Webbing"),
						HWSSettings.getMeasurement("TailOffset"), HWSSettings.getMeasurement("NoseOffset"));
				settingsDialog.dispose();
			}

		};
		printHWSMenu.add(printHWSSTringer);

		final AbstractAction printHWSRibs = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTHWSRIBS_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("SkinThickness", 0.4, LanguageResource.getString("HWSSKINTHICKNESS_STR"));
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				settings.getPreferences();
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("PRINTHWSRIBSTITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				mPrintHollowWoodTemplates.printCrosssectionTemplates(HWSSettings.getMeasurement("DistanceFromRail"),
						HWSSettings.getMeasurement("SkinThickness"), HWSSettings.getMeasurement("FrameThickness"),
						HWSSettings.getMeasurement("Webbing"));
				settingsDialog.dispose();
			}

		};
		printHWSMenu.add(printHWSRibs);

		final AbstractAction printHWSRail = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTHWSRAIL_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("SkinThickness", 0.4, LanguageResource.getString("HWSSKINTHICKNESS_STR"));
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				settings.getPreferences();
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("PRINTHWSRAILTITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				mPrintHollowWoodTemplates.printRailTemplate(HWSSettings.getMeasurement("DistanceFromRail"),
						HWSSettings.getMeasurement("SkinThickness"), HWSSettings.getMeasurement("FrameThickness"),
						HWSSettings.getMeasurement("Webbing"), HWSSettings.getMeasurement("TailOffset"),
						HWSSettings.getMeasurement("NoseOffset"));
				settingsDialog.dispose();
			}

		};
		printHWSMenu.add(printHWSRail);

		final AbstractAction printHWSNosePiece = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTHWSNOSEPIECE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("SkinThickness", 0.4, LanguageResource.getString("HWSSKINTHICKNESS_STR"));
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				settings.getPreferences();
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("PRINTHWSTAILPIECETITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				mPrintHollowWoodTemplates.printNoseTemplate(HWSSettings.getMeasurement("DistanceFromRail"),
						HWSSettings.getMeasurement("SkinThickness"), HWSSettings.getMeasurement("FrameThickness"),
						HWSSettings.getMeasurement("Webbing"), HWSSettings.getMeasurement("TailOffset"),
						HWSSettings.getMeasurement("NoseOffset"));
				settingsDialog.dispose();
			}

		};
		printHWSMenu.add(printHWSNosePiece);

		final AbstractAction printHWSTailPiece = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTHWSTAILPIECE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("SkinThickness", 0.4, LanguageResource.getString("HWSSKINTHICKNESS_STR"));
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				settings.getPreferences();
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("PRINTHWSNOSEPIECETITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				mPrintHollowWoodTemplates.printTailTemplate(HWSSettings.getMeasurement("DistanceFromRail"),
						HWSSettings.getMeasurement("SkinThickness"), HWSSettings.getMeasurement("FrameThickness"),
						HWSSettings.getMeasurement("Webbing"), HWSSettings.getMeasurement("TailOffset"),
						HWSSettings.getMeasurement("NoseOffset"));
				settingsDialog.dispose();
			}

		};
		printHWSMenu.add(printHWSTailPiece);

		final AbstractAction printHWSDeckTemplate = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTHWSDECKTEMPLATE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				settings.getPreferences();
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("PRINTHWSDECKTEMPLATETITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				mPrintHollowWoodTemplates.printDeckSkinTemplate(HWSSettings.getMeasurement("DistanceFromRail"),
						HWSSettings.getMeasurement("TailOffset"), HWSSettings.getMeasurement("NoseOffset"));
				settingsDialog.dispose();
			}

		};
		printHWSMenu.add(printHWSDeckTemplate);

		final AbstractAction printHWSBottomTemplate = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTHWSBOTTOMTEMPLATE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				settings.getPreferences();
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("PRINTHWSBOTTOMTEMPLATETITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				mPrintHollowWoodTemplates.printBottomSkinTemplate(HWSSettings.getMeasurement("DistanceFromRail"),
						HWSSettings.getMeasurement("TailOffset"), HWSSettings.getMeasurement("NoseOffset"));
				settingsDialog.dispose();
			}

		};
		printHWSMenu.add(printHWSBottomTemplate);

		printMenu.add(printHWSMenu);

		final JMenu printChamberedWoodMenu = new JMenu(
				LanguageResource.getString("PRINTCHAMBEREDWOODTEMPLATESMENU_STR"));

		final AbstractAction printChamberedWoodTemplate = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTCHAMBEREDWOODPROFILETEMPLATE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				final CategorizedSettings settings = new CategorizedSettings();
				final String categoryName = LanguageResource.getString("CHAMBEREDWOODPARAMETERSCATEGORY_STR");
				final Settings chamberedWoodSettings = settings.addCategory(categoryName);
				chamberedWoodSettings.addBoolean("Draw grid", true, LanguageResource.getString("DRAWGRID_STR"));
				chamberedWoodSettings.addMeasurement("Start Offset from center", 0.0,
						LanguageResource.getString("CHAMBEREDWOODOFFSETFROMCENTER_STR"));
				chamberedWoodSettings.addMeasurement("End Offset from center", mCurrentBrd.getMaxWidth() / 2.0,
						LanguageResource.getString("CHAMBEREDWOODENDOFFSET_STR"));
				chamberedWoodSettings.addMeasurement("Plank thickness", 2.54,
						LanguageResource.getString("CHAMBEREDWOODPLANKTHICKNESS_STR"));
				chamberedWoodSettings.addMeasurement("Deck/Bottom thickness", 0.8,
						LanguageResource.getString("CHAMBEREDWOODDECKANDBOTTOMTHICKNESS_STR"));
				chamberedWoodSettings.addBoolean("Draw chambering", true,
						LanguageResource.getString("CHAMBEREDDRAWCHAMBERING_STR"));
				chamberedWoodSettings.addBoolean("Draw alignment marks", true,
						LanguageResource.getString("CHAMBEREDDRAWALIGNEMNETMARKS_STR"));

				chamberedWoodSettings.addBoolean("Print multiple", false,
						LanguageResource.getString("CHAMBEREDPRINTMULTIPLETEMPLATES_STR"));

				settings.getPreferences();

				if (chamberedWoodSettings.getMeasurement("End Offset from center") > mCurrentBrd.getMaxWidth() / 2.0) {
					chamberedWoodSettings.setMeasurement("End Offset from center", mCurrentBrd.getMaxWidth() / 2.0);
				}

				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("PRINTCHAMBEREDWOODPROFILETEMPLATETITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}

				double start = chamberedWoodSettings.getMeasurement("Start Offset from center");
				double end = chamberedWoodSettings.getMeasurement("End Offset from center");
				double plankThickness = chamberedWoodSettings.getMeasurement("Plank thickness");

				boolean printMultiple = chamberedWoodSettings.getBoolean("Print multiple");
				if (printMultiple) {

					int numberOfTemplates = (int) ((end - start) / plankThickness);

					int selection = JOptionPane.showConfirmDialog(BoardCAD.getInstance().getFrame(),
							String.valueOf(numberOfTemplates) + " "
									+ LanguageResource.getString("PRINTCHAMBEREDWOODMULTIPLETEMPLATESMSG_STR"),
							LanguageResource.getString("PRINTCHAMBEREDWOODMULTIPLETEMPLATESTITLE_STR"),
							JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);
					if (selection != JOptionPane.YES_OPTION) {

						return;

					}
				}

				mPrintChamberedWoodTemplate.printTemplate(chamberedWoodSettings.getBoolean("Draw grid"), start, end,
						plankThickness, chamberedWoodSettings.getMeasurement("Deck/Bottom thickness"),
						chamberedWoodSettings.getBoolean("Draw chambering"),
						chamberedWoodSettings.getBoolean("Draw alignment marks"), printMultiple);

				settingsDialog.dispose();

				settings.putPreferences();
			}

		};
		printChamberedWoodMenu.add(printChamberedWoodTemplate);

		printMenu.add(printChamberedWoodMenu);

		printMenu.addSeparator();

		/*
		 * final JMenuItem printSpecSheet = new
		 * JMenuItem(LanguageResource.getString
		 * ("PRINTSPECSHEET_STR"),KeyEvent.VK_H);
		 * printSpecSheet.setAccelerator(KeyStroke
		 * .getKeyStroke(KeyEvent.VK_5,ActionEvent.ALT_MASK));
		 * printSpecSheet.addActionListener(this);
		 * printMenu.add(printSpecSheet);
		 */

		final AbstractAction printSpecSheet = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTSPECSHEET_STR"));
				this.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("PRINTSPECSHEET_STR"));
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_5, ActionEvent.ALT_MASK));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				mPrintSpecSheet.printSpecSheet();
			}

		};

		printMenu.add(printSpecSheet);

		mPrintBrd = new PrintBrd();
		mPrintSpecSheet = new PrintSpecSheet();
		mPrintChamberedWoodTemplate = new PrintChamberedWoodTemplate();
		mPrintSandwichTemplates = new PrintSandwichTemplates();
		mPrintHollowWoodTemplates = new PrintHollowWoodTemplates();

		final AbstractAction printSpecSheetToFile = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PRINTSPECSHEETTOFILE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				TwoValuesInputDialog resDialog = new TwoValuesInputDialog(mFrame);
				resDialog.setMessageText(LanguageResource.getString("PRINTSPECSHEETTOFILERESOLUTIONMSG_STR"));
				resDialog.setValue1(1200);
				resDialog.setValue2(1600);
				resDialog.setValue1LabelText(LanguageResource.getString("PRINTSPECSHEETTOFILEWIDTH_STR"));
				resDialog.setValue2LabelText(LanguageResource.getString("PRINTSPECSHEETTOFILEHEIGHT_STR"));
				resDialog.setModal(true);

				resDialog.setVisible(true);
				if (resDialog.wasCancelled()) {
					resDialog.dispose();
					return;
				}
				int width = 0, height = 0;
				try {
					width = (int) resDialog.getValue1();
					height = (int) resDialog.getValue2();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(),
							LanguageResource.getString("PRINTSPECSHEETTOFILEINVALIDPARAMETERSMSG_STR"),
							LanguageResource.getString("PRINTSPECSHEETTOFILEINVALIDPARAMETERSTITLE_STR"),
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				BufferedImage img = new BufferedImage(height, width, BufferedImage.TYPE_INT_RGB);
				Graphics2D g = img.createGraphics();

				Graphics2D g2d = (Graphics2D) g.create();

				// Turn on antialiasing, so painting is smooth.

				g2d.setRenderingHint(

						RenderingHints.KEY_ANTIALIASING,

						RenderingHints.VALUE_ANTIALIAS_ON);

				Paper paper = new Paper();
				paper.setImageableArea(0, 0, width, height);
				paper.setSize(width, height);
				PageFormat myPageFormat = new PageFormat();
				myPageFormat.setPaper(paper);
				myPageFormat.setOrientation(PageFormat.LANDSCAPE);

				g2d.setColor(Color.WHITE);
				g2d.fillRect(0, 0, height - 1, width - 1);

				mPrintSpecSheet.print(g2d, myPageFormat, 0);

				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				// Create a file dialog box to prompt for a new file to display
				FileFilter filter = new FileFilter() {

					// Accept all directories and graphics files.
					@Override
					public boolean accept(File f) {
						if (f.isDirectory()) {
							return true;
						}

						String extension = FileTools.getExtension(f);
						if (extension != null && (extension.equals("png") || extension.equals("gif")
								|| extension.equals("bmp") || extension.equals("jpg"))) {
							return true;
						}

						return false;
					}

					// The description of this filter
					@Override
					public String getDescription() {
						return LanguageResource.getString("PRINTSPECSHEETTOFILEIMAGEFILES_STR");
					}
				};

				fc.setFileFilter(filter);

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				if (FileTools.getExtension(filename) == "") {
					filename = FileTools.setExtension(filename, "jpg");
				}

				BoardCAD.defaultDirectory = file.getPath();

				try {
					File outputfile = new File(filename);
					ImageIO.write(img, FileTools.getExtension(filename), outputfile);
				} catch (Exception e) {
					String str = LanguageResource.getString("PRINTSPECSHEETTOFILEERRORMSG_STR") + e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("PRINTSPECSHEETTOFILEERRORTITLE_STR"),
							JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		printMenu.add(printSpecSheetToFile);

		fileMenu.add(printMenu);

		final JMenu importMenu = new JMenu(LanguageResource.getString("IMPORTMENU_STR"));

		final JMenu importBezierMenu = new JMenu(LanguageResource.getString("IMPORTBEZIERMENU_STR"));
		importMenu.add(importBezierMenu);
		final AbstractAction importOutlineAction = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("IMPORTBEZIEROUTLINE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				BrdImportOutlineCommand cmd = new BrdImportOutlineCommand(mOutlineEdit);
				cmd.execute();
			}

		};
		importBezierMenu.add(importOutlineAction);
		final AbstractAction importProfileAction = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("IMPORTBEZIERPROFILE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				BrdImportProfileCommand cmd = new BrdImportProfileCommand(mBottomAndDeckEdit);
				cmd.execute();
			}

		};
		importBezierMenu.add(importProfileAction);
		final AbstractAction importCrossSectionAction = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("IMPORTBEZIERCROSSSECTION_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				BrdImportCrossSectionCommand cmd = new BrdImportCrossSectionCommand(mCrossSectionEdit);
				cmd.execute();
			}

		};
		importBezierMenu.add(importCrossSectionAction);

		fileMenu.add(importMenu);

		final JMenu exportMenu = new JMenu(LanguageResource.getString("EXPORTMENU_STR"));

		final AbstractAction exportBezierStl = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("EXPORTBEZIERTOSTL_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				BoardCAD.defaultDirectory = file.getPath();

				try {
					StlExport.exportBezierBoard(filename, BoardCAD.getInstance().getCurrentBrd(), 50, 50, 200);
				} catch (Exception e) {
					String str = LanguageResource.getString("EXPORTBEZIERTOSTLFAILEDTITLE_STR") + e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("EXPORTBEZIERTOSTLFAILEDTITLE_STR"), JOptionPane.ERROR_MESSAGE);
				}
			}

		};

		exportMenu.add(exportBezierStl);
		exportMenu.addSeparator();

		JMenu beziersExportMenu = new JMenu(LanguageResource.getString("EXPORTBEZIERS_STR"));
		final AbstractAction exportBezierOutline = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("EXPORTBEZIEROUTLINE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));
				fc.setFileFilter(new FileFilter() {

					// Accept all directories and brd and s3d files.
					@Override
					public boolean accept(File f) {
						if (f.isDirectory()) {
							return true;
						}

						String extension = FileTools.getExtension(f);
						if (extension != null && extension.equals("otl")) {
							return true;
						}

						return false;
					}

					// The description of this filter
					@Override
					public String getDescription() {
						return LanguageResource.getString("OUTLINEFILES_STR");
					}
				});

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				try {
					if (BrdWriter.exportOutline(getCurrentBrd(), filename) == false) {
						throw new Exception();
					}
				} catch (Exception e) {
					String str = LanguageResource.getString("EXPORTBEZIEROUTLINEFAILEDMSG_STR") + e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("EXPORTBEZIEROUTLINEFAILEDTITLE_STR"),
							JOptionPane.ERROR_MESSAGE);

				}

				BoardCAD.defaultDirectory = file.getPath();

			}

		};
		beziersExportMenu.add(exportBezierOutline);

		final AbstractAction exportBezierProfile = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("EXPORTBEZIERPROFILE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));
				fc.setFileFilter(new FileFilter() {

					// Accept all directories and pfl files.
					@Override
					public boolean accept(File f) {
						if (f.isDirectory()) {
							return true;
						}

						String extension = FileTools.getExtension(f);
						if (extension != null && extension.equals("pfl")) {
							return true;
						}

						return false;
					}

					// The description of this filter
					@Override
					public String getDescription() {
						return LanguageResource.getString("PROFILEFILES_STR");
					}
				});

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				try {
					if (BrdWriter.exportProfile(getCurrentBrd(), filename) == false) {
						throw new Exception();
					}
				} catch (Exception e) {
					String str = LanguageResource.getString("EXPORTBEZIERFAILEDMSG_STR") + e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("EXPORTBEZIERFAILEDTITLE_STR"), JOptionPane.ERROR_MESSAGE);

				}

				BoardCAD.defaultDirectory = file.getPath();

			}

		};
		beziersExportMenu.add(exportBezierProfile);

		final AbstractAction exportBezierCrossection = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("EXPORTBEZIERCROSSECTION_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));
				fc.setFileFilter(new FileFilter() {

					// Accept all directories and brd and s3d files.
					@Override
					public boolean accept(File f) {
						if (f.isDirectory()) {
							return true;
						}

						String extension = FileTools.getExtension(f);
						if (extension != null && extension.equals("crs")) {
							return true;
						}

						return false;
					}

					// The description of this filter
					@Override
					public String getDescription() {
						return LanguageResource.getString("CROSSECTIONFILES_STR");
					}
				});

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				try {
					if (BrdWriter.exportCrossection(getCurrentBrd(), getCurrentBrd().getCurrentCrossSectionIndex(),
							filename) == false) {
						throw new Exception();
					}
				} catch (Exception e) {
					String str = LanguageResource.getString("EXPORTBEZIERFAILEDMSG_STR") + e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("EXPORTBEZIERFAILEDTITLE_STR"), JOptionPane.ERROR_MESSAGE);

				}

				BoardCAD.defaultDirectory = file.getPath();

			}

		};
		beziersExportMenu.add(exportBezierCrossection);

		exportMenu.add(beziersExportMenu);
		exportMenu.addSeparator();

		final AbstractAction exportProfileAsDxfSpline = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("EXPORTBEZIERPROFILEASDXFSPLINE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				BoardCAD.defaultDirectory = file.getPath();

				try {
					BezierSpline[] patches = new BezierSpline[2];
					patches[0] = BoardCAD.getInstance().getCurrentBrd().getBottom();
					patches[1] = new BezierSpline();
					BezierSpline org = BoardCAD.getInstance().getCurrentBrd().getDeck();

					for (int i = 0; i < org.getNrOfControlPoints(); i++) {
						BezierKnot controlPoint = (BezierKnot) org.getControlPoint((org.getNrOfControlPoints() - 1) - i)
								.clone();
						controlPoint.switch_tangents();
						patches[1].append(controlPoint);
					}

					DxfExport.exportBezierSplines(filename, patches);
				} catch (Exception e) {
					String str = LanguageResource.getString("EXPORTBEZIERPROFILEASDXFSPLINEFAILEDMSG_STR")
							+ e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("EXPORTBEZIERPROFILEASDXFSPLINEFAILEDTITLE_STR"),
							JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		exportMenu.add(exportProfileAsDxfSpline);

		final AbstractAction exportOutlineAsDxfSpline = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("EXPORTBEZIEROUTLINEASDXFSPLINE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				BoardCAD.defaultDirectory = file.getPath();

				try {
					BezierSpline[] patches = new BezierSpline[2];
					patches[0] = BoardCAD.getInstance().getCurrentBrd().getOutline();
					patches[1] = new BezierSpline();
					BezierSpline org = BoardCAD.getInstance().getCurrentBrd().getOutline();

					for (int i = 0; i < org.getNrOfControlPoints(); i++) {
						BezierKnot controlPoint = (BezierKnot) org.getControlPoint((org.getNrOfControlPoints() - 1) - i)
								.clone();
						controlPoint.switch_tangents();
						controlPoint.getEndPoint().y = -controlPoint.getEndPoint().y;
						controlPoint.getTangentToPrev().y = -controlPoint.getTangentToPrev().y;
						controlPoint.getTangentToNext().y = -controlPoint.getTangentToNext().y;
						patches[1].append(controlPoint);
					}
					DxfExport.exportBezierSplines(filename, patches);
				} catch (Exception e) {
					String str = LanguageResource.getString("EXPORTBEZIEROUTLINEASDXFSPLINEFAILEDMSG_STR")
							+ e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("EXPORTBEZIEROUTLINEASDXFSPLINEFAILEDTITLE_STR"),
							JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		exportMenu.add(exportOutlineAsDxfSpline);

		final AbstractAction exportCurrentCrossSectionAsDxfSpline = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("EXPORTBEZIERCROSSSECTIONASDXFSPLINE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				BoardCAD.defaultDirectory = file.getPath();

				try {
					BezierSpline[] patches = new BezierSpline[2];
					patches[0] = BoardCAD.getInstance().getCurrentBrd().getCurrentCrossSection().getBezierSpline();
					patches[1] = new BezierSpline();
					BezierSpline org = BoardCAD.getInstance().getCurrentBrd().getCurrentCrossSection()
							.getBezierSpline();

					for (int i = 0; i < org.getNrOfControlPoints(); i++) {
						BezierKnot controlPoint = (BezierKnot) org.getControlPoint((org.getNrOfControlPoints() - 1) - i)
								.clone();
						controlPoint.switch_tangents();
						controlPoint.getEndPoint().x = -controlPoint.getEndPoint().x;
						controlPoint.getTangentToPrev().x = -controlPoint.getTangentToPrev().x;
						controlPoint.getTangentToNext().x = -controlPoint.getTangentToNext().x;
						patches[1].append(controlPoint);
					}
					DxfExport.exportBezierSplines(filename, patches);
				} catch (Exception e) {
					String str = LanguageResource.getString("EXPORTBEZIERCROSSSECTIONASDXFSPLINEFAILEDMSG_STR")
							+ e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("EXPORTBEZIERCROSSSECTIONASDXFSPLINEFAILEDTITLE_STR"),
							JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		exportMenu.add(exportCurrentCrossSectionAsDxfSpline);

		exportMenu.addSeparator();

		final AbstractAction exportProfileAsDxfPolyline = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("EXPORTBEZIERPROFILEASDXFPOLYLINE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				BoardCAD.defaultDirectory = file.getPath();

				try {
					BezierSpline[] patches = new BezierSpline[2];
					patches[0] = BoardCAD.getInstance().getCurrentBrd().getBottom();
					patches[1] = new BezierSpline();
					BezierSpline org = BoardCAD.getInstance().getCurrentBrd().getDeck();

					for (int i = 0; i < org.getNrOfControlPoints(); i++) {
						BezierKnot controlPoint = (BezierKnot) org.getControlPoint((org.getNrOfControlPoints() - 1) - i)
								.clone();
						controlPoint.switch_tangents();
						patches[1].append(controlPoint);
					}

					DxfExport.exportPolylineFromSplines(filename, patches, 100);
				} catch (Exception e) {
					String str = LanguageResource.getString("EXPORTBEZIERPROFILEASDXFPOLYLINEFAILEDMSG_STR")
							+ e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("EXPORTBEZIERPROFILEASDXFPOLYLINEFAILEDTITLE_STR"),
							JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		exportMenu.add(exportProfileAsDxfPolyline);

		final AbstractAction exportOutlineAsDxfPolyline = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("EXPORTBEZIEROUTLINEASDXFPOLYLINE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				BoardCAD.defaultDirectory = file.getPath();

				try {
					BezierSpline[] patches = new BezierSpline[2];
					patches[0] = BoardCAD.getInstance().getCurrentBrd().getOutline();
					patches[1] = new BezierSpline();
					BezierSpline org = BoardCAD.getInstance().getCurrentBrd().getOutline();

					for (int i = 0; i < org.getNrOfControlPoints(); i++) {
						BezierKnot controlPoint = (BezierKnot) org.getControlPoint((org.getNrOfControlPoints() - 1) - i)
								.clone();
						controlPoint.switch_tangents();
						controlPoint.getEndPoint().y = -controlPoint.getEndPoint().y;
						controlPoint.getTangentToPrev().y = -controlPoint.getTangentToPrev().y;
						controlPoint.getTangentToNext().y = -controlPoint.getTangentToNext().y;
						patches[1].append(controlPoint);
					}
					DxfExport.exportPolylineFromSplines(filename, patches, 100);
				} catch (Exception e) {
					String str = LanguageResource.getString("EXPORTBEZIEROUTLINEASDXFPOLYLINEFAILEDMSG_STR")
							+ e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("EXPORTBEZIEROUTLINEASDXFPOLYLINEFAILEDTITLE_STR"),
							JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		exportMenu.add(exportOutlineAsDxfPolyline);

		final AbstractAction exportCrossSectionAsDxfPolyline = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("EXPORTBEZIERCROSSSECTIONASDXFPOLYLINE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				BoardCAD.defaultDirectory = file.getPath();

				try {
					BezierSpline[] patches = new BezierSpline[2];
					patches[0] = BoardCAD.getInstance().getCurrentBrd().getCurrentCrossSection().getBezierSpline();
					patches[1] = new BezierSpline();
					BezierSpline org = BoardCAD.getInstance().getCurrentBrd().getCurrentCrossSection()
							.getBezierSpline();

					for (int i = 0; i < org.getNrOfControlPoints(); i++) {
						BezierKnot controlPoint = (BezierKnot) org.getControlPoint((org.getNrOfControlPoints() - 1) - i)
								.clone();
						controlPoint.switch_tangents();
						controlPoint.getEndPoint().x = -controlPoint.getEndPoint().x;
						controlPoint.getTangentToPrev().x = -controlPoint.getTangentToPrev().x;
						controlPoint.getTangentToNext().x = -controlPoint.getTangentToNext().x;
						patches[1].append(controlPoint);
					}
					DxfExport.exportPolylineFromSplines(filename, patches, 100);
				} catch (Exception e) {
					String str = LanguageResource.getString("EXPORTBEZIERCROSSSECTIONASDXFPOLYLINEFAILEDMSG_STR")
							+ e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("EXPORTBEZIERCROSSECTIONASDXFPOLYLINEFAILEDTITLE_STR"),
							JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		exportMenu.add(exportCrossSectionAsDxfPolyline);

		fileMenu.add(exportMenu);

		final JMenu gcodeMenu = new JMenu(LanguageResource.getString("GCODEMENU_STR"));

		final AbstractAction gcodeBezier = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEBEZIER_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				BezierBoard brd = getCurrentBrd();
				MachineConfig machineConfig = new MachineConfig();
				machineConfig.setBoard((BezierBoard) brd.clone());
				MachineDialog dialog = new MachineDialog(machineConfig);
				// dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
				// dialog.setModal(false);

				machineConfig.setMachineView(dialog.getMachineView());
				machineConfig.initialize();
				machineConfig.getPreferences();

				// Turn of sandwich compensation so we don't use sandwich
				// compensation by accident (lesson learned the hard way)
				Settings sandwichCompensationSettings = machineConfig
						.getCategory(LanguageResource.getString("SANDWICHCOMPENSATIONCATEGORY_STR"));
				sandwichCompensationSettings.setBoolean(SandwichCompensation.SANDWICH_DECK_COMPENSATION_ON, false);
				sandwichCompensationSettings.setBoolean(SandwichCompensation.SANDWICH_BOTTOM_COMPENSATION_ON, false);
				sandwichCompensationSettings.setBoolean(SandwichCompensation.SANDWICH_OUTLINE_COMPENSATION_ON, false);

				Settings generalSettings = machineConfig.getCategory(LanguageResource.getString("GENERALCATEGORY_STR"));

				if (generalSettings.getBoolean(MachineConfig.USE_BRD_SETTINGS) == true) {
					System.out.printf("Using board settings");
					if (generalSettings.getEnumeration(MachineConfig.BLANKHOLDINGSYSTEM_TYPE) == 0) {
						// generalSettings.getDouble(MachineConfig.TAILSTOP_POS,
						// );

						Settings supportsSettings = machineConfig
								.getCategory(LanguageResource.getString("BLANKHOLDINGSYSTEMCATEGORY_STR"));

						supportsSettings.setObject(SupportsBlankHoldingSystem.SUPPORT_1_POS,
								new Double(brd.getStrut1()[0]));
						supportsSettings.setObject(SupportsBlankHoldingSystem.SUPPORT_2_POS,
								new Double(brd.getStrut2()[0]));

						supportsSettings.setObject(SupportsBlankHoldingSystem.SUPPORT_1_HEIGHT,
								new Double(brd.getStrut1()[1]));
						supportsSettings.setObject(SupportsBlankHoldingSystem.SUPPORT_2_HEIGHT,
								new Double(brd.getStrut2()[1]));
					}

					generalSettings.setObject(MachineConfig.BLANK, generalSettings.new FileName(brd.getBlankFile()));

					Settings cutsSettings = machineConfig.getCategory(LanguageResource.getString("CUTSCATEGORY_STR"));

					cutsSettings.setObject(MachineConfig.DECK_CUTS, new Integer(brd.getTopCuts()));
					cutsSettings.setObject(MachineConfig.DECK_RAIL_CUTS, new Integer(brd.getTopShoulderCuts()));
					cutsSettings.setObject(MachineConfig.BOTTOM_CUTS, new Integer(brd.getBottomCuts()));
					cutsSettings.setObject(MachineConfig.BOTTOM_RAIL_CUTS, new Integer(brd.getBottomRailCuts()));

					cutsSettings.setObject(MachineConfig.DECK_ANGLE, new Double(brd.getTopShoulderAngle()));
					cutsSettings.setObject(MachineConfig.DECK_RAIL_ANGLE, new Double(brd.getMaxAngle()));

					Settings speedSettings = machineConfig.getCategory(LanguageResource.getString("SPEEDCATEGORY_STR"));
					speedSettings.setObject(MachineConfig.CUTTING_SPEED, new Double(brd.getRegularSpeed()));
					speedSettings.setObject(MachineConfig.CUTTING_SPEED_STRINGER, new Double(brd.getStringerSpeed()));
					speedSettings.setObject(MachineConfig.CUTTING_SPEED_RAIL, new Double(brd.getRegularSpeed()));
					speedSettings.setObject(MachineConfig.CUTTING_SPEED_OUTLINE, new Double(brd.getRegularSpeed()));

				}

				dialog.setVisible(true);

				if (generalSettings.getBoolean(MachineConfig.USE_BRD_SETTINGS)) {
					if (generalSettings.getEnumeration(MachineConfig.BLANKHOLDINGSYSTEM_TYPE) == 0) {
						// generalSettings.getDouble(MachineConfig.TAILSTOP_POS,
						// );

						Settings supportsSettings = machineConfig
								.addCategory(LanguageResource.getString("BLANKHOLDINGSYSTEMCATEGORY_STR"));

						// generalSettings.getDouble(MachineConfig.TAILSTOP_POS,
						// );
						brd.getStrut1()[0] = supportsSettings.getDouble(SupportsBlankHoldingSystem.SUPPORT_1_POS);
						brd.getStrut2()[0] = supportsSettings.getDouble(SupportsBlankHoldingSystem.SUPPORT_2_POS);

						brd.getStrut1()[1] = supportsSettings.getDouble(SupportsBlankHoldingSystem.SUPPORT_1_HEIGHT);
						brd.getStrut2()[1] = supportsSettings.getDouble(SupportsBlankHoldingSystem.SUPPORT_2_HEIGHT);
					}

					brd.setBlankFile(generalSettings.getFileName(MachineConfig.BLANK));

					Settings cutsSettings = machineConfig.getCategory(LanguageResource.getString("CUTSCATEGORY_STR"));

					brd.setTopCuts(cutsSettings.getInt(MachineConfig.DECK_CUTS));
					brd.setTopShoulderCuts(cutsSettings.getInt(MachineConfig.DECK_RAIL_CUTS));
					brd.setBottomCuts(cutsSettings.getInt(MachineConfig.BOTTOM_CUTS));
					brd.setBottomRailCuts(cutsSettings.getInt(MachineConfig.BOTTOM_RAIL_CUTS));

					brd.setTopShoulderAngle(cutsSettings.getDouble(MachineConfig.DECK_ANGLE));
					brd.setMaxAngle(cutsSettings.getDouble(MachineConfig.DECK_RAIL_ANGLE));
					// cutsSettings.getDouble(MachineConfig.BOTTOM_ANGLE, new
					// Double(90));
					// cutsSettings.getDouble(MachineConfig.BOTTOM_RAIL_ANGLE,
					// new Double(90));

					Settings speedSettings = machineConfig.getCategory(LanguageResource.getString("SPEEDCATEGORY_STR"));
					brd.setRegularSpeed((int) speedSettings.getDouble(MachineConfig.CUTTING_SPEED));
					brd.setStringerSpeed((int) speedSettings.getDouble(MachineConfig.CUTTING_SPEED_STRINGER));
					brd.setRegularSpeed((int) speedSettings.getDouble(MachineConfig.CUTTING_SPEED_RAIL));
					// speedSettings.getDouble(MachineConfig.CUTTING_SPEED_NOSE_REDUCTION,
					// new Double(0.5));
					// speedSettings.getDouble(MachineConfig.CUTTING_SPEED_TAIL_REDUCTION,
					// new Double(0.5));
					// brd.setNoseLength(speedSettings.getDouble(MachineConfig.CUTTING_SPEED_NOSE_REDUCTION_DIST));
					// brd.setTailLength(speedSettings.getDouble(MachineConfig.CUTTING_SPEED_TAIL_REDUCTION_DIST));
				}
			}
		};

		gcodeMenu.add(gcodeBezier);

		gcodeMenu.addSeparator();

		final AbstractAction gcodeOutline = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEOUTLINE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HOTWIRECATEGORY_STR");
				Settings hotwireSettings = settings.addCategory(categoryName);
				hotwireSettings.addMeasurement("CuttingSpeed", 50.0,
						LanguageResource.getString("HOTWIRECUTTINGSPEED_STR"));
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("HOTWIREPARAMETERSTITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				settingsDialog.dispose();
				if (settingsDialog.wasCancelled()) {
					return;
				}

				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				filename = FileTools.setExtension(filename, "nc");

				BoardCAD.defaultDirectory = file.getPath();

				HotwireToolpathGenerator2 toolpathGenerator = new HotwireToolpathGenerator2(new AbstractCutter() {
					public double[] calcOffset(Point3d point, Vector3d normal, AbstractBoard board) {
						return new double[] { point.x, point.y, point.z };
					}

				}, new GCodeWriter(),
						hotwireSettings.getMeasurement("CuttingSpeed") * UnitUtils.MILLIMETER_PR_CENTIMETER);

				try {
					toolpathGenerator.writeOutline(filename, getCurrentBrd());
				} catch (Exception e) {
					String str = LanguageResource.getString("GCODEOUTLINEFAILEDMSG_STR") + e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("GCODEOUTLINEFAILEDTITLE_STR"), JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		gcodeMenu.add(gcodeOutline);

		final AbstractAction gcodeProfile = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEPROFILE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HOTWIRECATEGORY_STR");
				final Settings hotwireSettings = settings.addCategory(categoryName);
				hotwireSettings.addMeasurement("CuttingSpeed", 50.0,
						LanguageResource.getString("HOTWIRECUTTINGSPEED_STR"));
				hotwireSettings.addMeasurement("AdditionalThickness", 0.0,
						LanguageResource.getString("ADDITIONALTHICKNESS_STR"));
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("HOTWIREPARAMETERSTITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				settingsDialog.dispose();
				if (settingsDialog.wasCancelled()) {
					return;
				}

				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				filename = FileTools.setExtension(filename, "nc");

				BoardCAD.defaultDirectory = file.getPath();

				HotwireToolpathGenerator2 toolpathGenerator = new HotwireToolpathGenerator2(new AbstractCutter() {
					public double[] calcOffset(Point3d point, Vector3d normal, AbstractBoard board) {

						// double additionalThickness =
						// hotwireSettings.getMeasurement("AdditionalThickness")*UnitUtils.MILLIMETER_PR_CENTIMETER;

						Point3d offsetPoint = new Point3d(point);
						// Vector3d normalScaled = new Vector3d(normal);
						// normalScaled.scale(additionalThickness);
						// offsetPoint.add(normalScaled);

						return new double[] { offsetPoint.x, offsetPoint.y, offsetPoint.z };
					}

				}, new GCodeWriter(),
						hotwireSettings.getMeasurement("CuttingSpeed") * UnitUtils.MILLIMETER_PR_CENTIMETER,
						hotwireSettings.getMeasurement("AdditionalThickness") * UnitUtils.MILLIMETER_PR_CENTIMETER);

				try {
					toolpathGenerator.writeProfile(filename, getCurrentBrd());
				} catch (Exception e) {
					String str = LanguageResource.getString("GCODEPROFILEFAILEDMSG_STR") + e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("GCODEPROFILEFAILEDTITLE_STR"), JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		gcodeMenu.add(gcodeProfile);

		final AbstractAction gcodeDeck = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEDECK_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				BoardCAD.defaultDirectory = file.getPath();

				MachineConfig config = new MachineConfig();
				config.getPreferences();

				AbstractToolpathGenerator toolpathGenerator = new WidthSplitsToolpathGenerator(new AbstractCutter() {
					public double[] calcOffset(Point3d point, Vector3d normal, AbstractBoard board) {
						return new double[] { point.x, point.y, point.z };
					}

					public double calcSpeed(Point3d point, Vector3d normal, AbstractBoard board,
							boolean isCuttingStringer) {
						return 10;
					}

				}, null, new GCodeWriter(), config);

				try {
					toolpathGenerator.writeToolpath(filename, getCurrentBrd(), null);
				} catch (Exception e) {
					String str = LanguageResource.getString("GCODEDECKFAILEDMSG_STR") + e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("GCODEDECKFAILEDTITLE_STR"), JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		gcodeMenu.add(gcodeDeck);

		final AbstractAction gcodeBottom = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEBOTTOM_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				BoardCAD.defaultDirectory = file.getPath();

				MachineConfig config = new MachineConfig();
				config.getPreferences();

				AbstractToolpathGenerator toolpathGenerator = new WidthSplitsToolpathGenerator(new AbstractCutter() {
					public double[] calcOffset(Point3d point, Vector3d normal, AbstractBoard board) {
						return new double[] { point.x, point.y, point.z };
					}

					public double calcSpeed(Point3d point, Vector3d normal, AbstractBoard board,
							boolean isCuttingStringer) {
						return 10;
					}

				}, null, new GCodeWriter(), config);

				try {
					toolpathGenerator.writeToolpath(filename, getCurrentBrd(), null);
				} catch (Exception e) {
					String str = LanguageResource.getString("GCODEBOTTOMFAILEDMSG_STR") + e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("GCODEBOTTOMFAILEDMSG_STR"), JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		gcodeMenu.add(gcodeBottom);

		final JMenu gcodeHWSMenu = new JMenu(LanguageResource.getString("GCODEHWSMENU_STR"));

		final AbstractAction gcodeHWSSTringer = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEHWSSTRINGER_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("SkinThickness", 0.4, LanguageResource.getString("HWSSKINTHICKNESS_STR"));
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("CutterDiam", 5.0, LanguageResource.getString("CUTTERDIAMETER_STR"));
				HWSSettings.addMeasurement("JogHeight", 5.0, LanguageResource.getString("JOGHEIGHT_STR"));
				HWSSettings.addMeasurement("JogSpeed", 20.0, LanguageResource.getString("JOGSPEED_STR"));
				HWSSettings.addMeasurement("SinkSpeed", 2.0, LanguageResource.getString("SINKSPEED_STR"));
				HWSSettings.addMeasurement("CutDepth", 0.0, LanguageResource.getString("CUTDEPTH_STR"));
				HWSSettings.addMeasurement("CutSpeed", 2.0, LanguageResource.getString("CUTSPEED_STR"));
				settings.getPreferences();
				String filename = FileTools.removeExtension(getCurrentBrd().getFilename());
				filename.concat("HWSStringer.nc");
				HWSSettings.addFileName("StringerFilename", filename, LanguageResource.getString("FILENAME_STR"));

				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("HWSSTRINGERTITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				GCodeDraw gdraw = new GCodeDraw(HWSSettings.getFileName("StringerFilename"),
						HWSSettings.getMeasurement("CutterDiam"), HWSSettings.getMeasurement("CutDepth"),
						HWSSettings.getMeasurement("CutSpeed"), HWSSettings.getMeasurement("JogHeight"),
						HWSSettings.getMeasurement("JogSpeed"), HWSSettings.getMeasurement("SinkSpeed"));

				double skinThickness = HWSSettings.getMeasurement("SkinThickness");
				double frameThickness = HWSSettings.getMeasurement("FrameThickness");
				double webbing = HWSSettings.getMeasurement("Webbing");
				double tailOffset = HWSSettings.getMeasurement("TailOffset");
				double noseOffset = HWSSettings.getMeasurement("NoseOffset");
				double distanceToRail = HWSSettings.getMeasurement("DistanceFromRail");

				gdraw.setFlipNormal(true);

				BezierBoardDrawUtil.printProfile(gdraw, 0.0, 0.0, 1.0, 0.0, false,
						BoardCAD.getInstance().getCurrentBrd(), 0.0, skinThickness, false, tailOffset, noseOffset);

				gdraw.setFlipNormal(false);

				PrintHollowWoodTemplates.printStringerWebbing(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), skinThickness, frameThickness, webbing);

				PrintHollowWoodTemplates.printStringerTailPieceCutOut(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset);

				PrintHollowWoodTemplates.printStringerNosePieceCutOut(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset);

				settingsDialog.dispose();
				gdraw.close();
			}

		};
		gcodeHWSMenu.add(gcodeHWSSTringer);

		final AbstractAction gcodeHWSRibs = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEHWSRIBS_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				LanguageResource.getString("HWSSKINTHICKNESS_STR");
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("CutterDiam", 5.0, LanguageResource.getString("CUTTERDIAMETER_STR"));
				HWSSettings.addMeasurement("JogHeight", 5.0, LanguageResource.getString("JOGHEIGHT_STR"));
				HWSSettings.addMeasurement("JogSpeed", 20.0, LanguageResource.getString("JOGSPEED_STR"));
				HWSSettings.addMeasurement("SinkSpeed", 2.0, LanguageResource.getString("SINKSPEED_STR"));
				HWSSettings.addMeasurement("CutDepth", 0.0, LanguageResource.getString("CUTDEPTH_STR"));
				HWSSettings.addMeasurement("CutSpeed", 2.0, LanguageResource.getString("CUTSPEED_STR"));
				settings.getPreferences();
				String filename = FileTools.removeExtension(getCurrentBrd().getFilename());
				filename.concat("HWSRibs.nc");
				HWSSettings.addFileName("Filename", filename, LanguageResource.getString("FILENAME_STR"));

				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("GCODEHWSRAILSTITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				GCodeDraw gdraw = new GCodeDraw(HWSSettings.getFileName("Filename"),
						HWSSettings.getMeasurement("CutterDiam"), HWSSettings.getMeasurement("CutDepth"),
						HWSSettings.getMeasurement("CutSpeed"), HWSSettings.getMeasurement("JogHeight"),
						HWSSettings.getMeasurement("JogSpeed"), HWSSettings.getMeasurement("SinkSpeed"));

				double skinThickness = HWSSettings.getMeasurement("SkinThickness");
				double frameThickness = HWSSettings.getMeasurement("FrameThickness");
				double webbing = HWSSettings.getMeasurement("Webbing");
				double tailOffset = HWSSettings.getMeasurement("TailOffset");
				double noseOffset = HWSSettings.getMeasurement("NoseOffset");
				double distanceToRail = HWSSettings.getMeasurement("DistanceFromRail");
				double cutterDiam = HWSSettings.getMeasurement("CutterDiam");

				//
				int nrOfCrossSections = (int) ((BoardCAD.getInstance().getCurrentBrd().getLength()
						- 9.0 * UnitUtils.INCH) / UnitUtils.FOOT);
				double crosssectionPos = 0.0;
				double verticalPos = 0.0;
				for (int i = 0; i < nrOfCrossSections; i++) {
					crosssectionPos = (i + 1) * UnitUtils.FOOT;

					PrintHollowWoodTemplates.printCrossSection(gdraw, 0.0, verticalPos, 1.0, 0.0,
							BoardCAD.getInstance().getCurrentBrd(), crosssectionPos, distanceToRail, skinThickness,
							frameThickness, webbing, false);

					PrintHollowWoodTemplates.printCrossSectionWebbing(gdraw, verticalPos, 0.0, 1.0, 0.0,
							BoardCAD.getInstance().getCurrentBrd(), crosssectionPos, distanceToRail, skinThickness,
							frameThickness, webbing, false);

					double verticalStep = BoardCAD.getInstance().getCurrentBrd().getThicknessAtPos(crosssectionPos)
							- skinThickness + (cutterDiam * 2.0);

					verticalPos += verticalStep;
				}

				settingsDialog.dispose();
			}

		};
		gcodeHWSMenu.add(gcodeHWSRibs);

		final AbstractAction gcodeHWSRail = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEHWSRAIL_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				LanguageResource.getString("HWSSKINTHICKNESS_STR");
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("CutterDiam", 5.0, LanguageResource.getString("CUTTERDIAMETER_STR"));
				HWSSettings.addMeasurement("JogHeight", 5.0, LanguageResource.getString("JOGHEIGHT_STR"));
				HWSSettings.addMeasurement("JogSpeed", 20.0, LanguageResource.getString("JOGSPEED_STR"));
				HWSSettings.addMeasurement("SinkSpeed", 2.0, LanguageResource.getString("SINKSPEED_STR"));
				HWSSettings.addMeasurement("CutDepth", 0.0, LanguageResource.getString("CUTDEPTH_STR"));
				HWSSettings.addMeasurement("CutSpeed", 2.0, LanguageResource.getString("CUTSPEED_STR"));
				settings.getPreferences();
				String filename = FileTools.removeExtension(getCurrentBrd().getFilename());
				filename.concat("HWSRibs.nc");
				HWSSettings.addFileName("StringerFilename", filename, LanguageResource.getString("FILENAME_STR"));

				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("GCODEHWSRAILSTITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				GCodeDraw gdraw = new GCodeDraw(HWSSettings.getFileName("StringerFilename"),
						HWSSettings.getMeasurement("CutterDiam"), HWSSettings.getMeasurement("CutDepth"),
						HWSSettings.getMeasurement("CutSpeed"), HWSSettings.getMeasurement("JogHeight"),
						HWSSettings.getMeasurement("JogSpeed"), HWSSettings.getMeasurement("SinkSpeed"));

				double skinThickness = HWSSettings.getMeasurement("SkinThickness");
				double frameThickness = HWSSettings.getMeasurement("FrameThickness");
				double webbing = HWSSettings.getMeasurement("Webbing");
				double tailOffset = HWSSettings.getMeasurement("TailOffset");
				double noseOffset = HWSSettings.getMeasurement("NoseOffset");
				double distanceToRail = HWSSettings.getMeasurement("DistanceFromRail");

				PrintHollowWoodTemplates.printRailWebbing(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset);

				PrintHollowWoodTemplates.printRailNotching(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset);

				PrintHollowWoodTemplates.printRailNosePieceNotches(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset);

				PrintHollowWoodTemplates.printRailTailPieceNotches(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset);

				settingsDialog.dispose();
			}

		};
		gcodeHWSMenu.add(gcodeHWSRail);

		final AbstractAction gcodeHWSNosePiece = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEHWSTAILPIECE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("SkinThickness", 0.4, LanguageResource.getString("HWSSKINTHICKNESS_STR"));
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("CutterDiam", 5.0, LanguageResource.getString("CUTTERDIAMETER_STR"));
				HWSSettings.addMeasurement("JogHeight", 5.0, LanguageResource.getString("JOGHEIGHT_STR"));
				HWSSettings.addMeasurement("JogSpeed", 20.0, LanguageResource.getString("JOGSPEED_STR"));
				HWSSettings.addMeasurement("SinkSpeed", 2.0, LanguageResource.getString("SINKSPEED_STR"));
				HWSSettings.addMeasurement("CutDepth", 0.0, LanguageResource.getString("CUTDEPTH_STR"));
				HWSSettings.addMeasurement("CutSpeed", 2.0, LanguageResource.getString("CUTSPEED_STR"));
				settings.getPreferences();
				String filename = FileTools.removeExtension(getCurrentBrd().getFilename());
				filename.concat("HWSTail.nc");
				HWSSettings.addFileName("StringerFilename", filename, LanguageResource.getString("FILENAME_STR"));

				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("GCODEHWSTAILPIECETITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				GCodeDraw gdraw = new GCodeDraw(HWSSettings.getFileName("StringerFilename"),
						HWSSettings.getMeasurement("CutterDiam"), HWSSettings.getMeasurement("CutDepth"),
						HWSSettings.getMeasurement("CutSpeed"), HWSSettings.getMeasurement("JogHeight"),
						HWSSettings.getMeasurement("JogSpeed"), HWSSettings.getMeasurement("SinkSpeed"));

				double skinThickness = HWSSettings.getMeasurement("SkinThickness");
				double frameThickness = HWSSettings.getMeasurement("FrameThickness");
				double webbing = HWSSettings.getMeasurement("Webbing");
				double tailOffset = HWSSettings.getMeasurement("TailOffset");
				double noseOffset = HWSSettings.getMeasurement("NoseOffset");
				double distanceToRail = HWSSettings.getMeasurement("DistanceFromRail");

				PrintHollowWoodTemplates.printNosePiece(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset, false);

				PrintHollowWoodTemplates.printNosePiece(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset, true);

				PrintHollowWoodTemplates.printNosePieceWebbing(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset, false);

				PrintHollowWoodTemplates.printNosePieceWebbing(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset, true);

				settingsDialog.dispose();
			}

		};
		gcodeHWSMenu.add(gcodeHWSNosePiece);

		final AbstractAction gcodeHWSTailPiece = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEHWSNOSEPIECE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("SkinThickness", 0.4, LanguageResource.getString("HWSSKINTHICKNESS_STR"));
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("CutterDiam", 5.0, LanguageResource.getString("CUTTERDIAMETER_STR"));
				HWSSettings.addMeasurement("JogHeight", 5.0, LanguageResource.getString("JOGHEIGHT_STR"));
				HWSSettings.addMeasurement("JogSpeed", 20.0, LanguageResource.getString("JOGSPEED_STR"));
				HWSSettings.addMeasurement("SinkSpeed", 2.0, LanguageResource.getString("SINKSPEED_STR"));
				HWSSettings.addMeasurement("CutDepth", 0.0, LanguageResource.getString("CUTDEPTH_STR"));
				HWSSettings.addMeasurement("CutSpeed", 2.0, LanguageResource.getString("CUTSPEED_STR"));
				settings.getPreferences();
				String filename = FileTools.removeExtension(getCurrentBrd().getFilename());
				filename.concat("HWSNose.nc");
				HWSSettings.addFileName("StringerFilename", filename, LanguageResource.getString("FILENAME_STR"));

				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("GCODEHWSNOSEPIECETITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				GCodeDraw gdraw = new GCodeDraw(HWSSettings.getFileName("StringerFilename"),
						HWSSettings.getMeasurement("CutterDiam"), HWSSettings.getMeasurement("CutDepth"),
						HWSSettings.getMeasurement("CutSpeed"), HWSSettings.getMeasurement("JogHeight"),
						HWSSettings.getMeasurement("JogSpeed"), HWSSettings.getMeasurement("SinkSpeed"));

				double skinThickness = HWSSettings.getMeasurement("SkinThickness");
				double frameThickness = HWSSettings.getMeasurement("FrameThickness");
				double webbing = HWSSettings.getMeasurement("Webbing");
				double tailOffset = HWSSettings.getMeasurement("TailOffset");
				double noseOffset = HWSSettings.getMeasurement("NoseOffset");
				double distanceToRail = HWSSettings.getMeasurement("DistanceFromRail");

				PrintHollowWoodTemplates.printTailPiece(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset, false);

				PrintHollowWoodTemplates.printTailPiece(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset, true);

				PrintHollowWoodTemplates.printTailPieceWebbing(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset, false);

				PrintHollowWoodTemplates.printTailPieceWebbing(gdraw, 0.0, 0.0, 1.0, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset, true);

				settingsDialog.dispose();
			}

		};
		gcodeHWSMenu.add(gcodeHWSTailPiece);

		final AbstractAction gcodeHWSDeckTemplate = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEHWSDECKTEMPLATE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("SkinThickness", 0.4, LanguageResource.getString("HWSSKINTHICKNESS_STR"));
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("CutterDiam", 5.0, LanguageResource.getString("CUTTERDIAMETER_STR"));
				HWSSettings.addMeasurement("JogHeight", 5.0, LanguageResource.getString("JOGHEIGHT_STR"));
				HWSSettings.addMeasurement("JogSpeed", 20.0, LanguageResource.getString("JOGSPEED_STR"));
				HWSSettings.addMeasurement("SinkSpeed", 2.0, LanguageResource.getString("SINKSPEED_STR"));
				HWSSettings.addMeasurement("CutDepth", 0.0, LanguageResource.getString("CUTDEPTH_STR"));
				HWSSettings.addMeasurement("CutSpeed", 2.0, LanguageResource.getString("CUTSPEED_STR"));
				settings.getPreferences();
				String filename = FileTools.removeExtension(getCurrentBrd().getFilename());
				filename.concat("HWSRibs.nc");
				HWSSettings.addFileName("StringerFilename", filename, LanguageResource.getString("FILENAME_STR"));

				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("HWSSTRINGERTITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				GCodeDraw gdraw = new GCodeDraw(HWSSettings.getFileName("StringerFilename"),
						HWSSettings.getMeasurement("CutterDiam"), HWSSettings.getMeasurement("CutDepth"),
						HWSSettings.getMeasurement("CutSpeed"), HWSSettings.getMeasurement("JogHeight"),
						HWSSettings.getMeasurement("JogSpeed"), HWSSettings.getMeasurement("SinkSpeed"));

				double skinThickness = HWSSettings.getMeasurement("SkinThickness");
				double frameThickness = HWSSettings.getMeasurement("FrameThickness");
				double webbing = HWSSettings.getMeasurement("Webbing");
				double tailOffset = HWSSettings.getMeasurement("TailOffset");
				double noseOffset = HWSSettings.getMeasurement("NoseOffset");
				double distanceToRail = HWSSettings.getMeasurement("DistanceFromRail");

				BezierBoardDrawUtil.printDeckSkinTemplate(gdraw, 0.0, 0.0, 1.0, 0.0, true,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail);

				settingsDialog.dispose();
			}

		};
		gcodeHWSMenu.add(gcodeHWSDeckTemplate);

		final AbstractAction gcodeHWSBottomTemplate = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEHWSBOTTOMTEMPLATE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("SkinThickness", 0.4, LanguageResource.getString("HWSSKINTHICKNESS_STR"));
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("CutterDiam", 5.0, LanguageResource.getString("CUTTERDIAMETER_STR"));
				HWSSettings.addMeasurement("JogHeight", 5.0, LanguageResource.getString("JOGHEIGHT_STR"));
				HWSSettings.addMeasurement("JogSpeed", 20.0, LanguageResource.getString("JOGSPEED_STR"));
				HWSSettings.addMeasurement("SinkSpeed", 2.0, LanguageResource.getString("SINKSPEED_STR"));
				HWSSettings.addMeasurement("CutDepth", 0.0, LanguageResource.getString("CUTDEPTH_STR"));
				HWSSettings.addMeasurement("CutSpeed", 2.0, LanguageResource.getString("CUTSPEED_STR"));
				settings.getPreferences();
				String filename = FileTools.removeExtension(getCurrentBrd().getFilename());
				filename.concat("HWSRibs.nc");
				HWSSettings.addFileName("StringerFilename", filename, LanguageResource.getString("FILENAME_STR"));

				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("HWSSTRINGERTITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				GCodeDraw gdraw = new GCodeDraw(HWSSettings.getFileName("StringerFilename"),
						HWSSettings.getMeasurement("CutterDiam"), HWSSettings.getMeasurement("CutDepth"),
						HWSSettings.getMeasurement("CutSpeed"), HWSSettings.getMeasurement("JogHeight"),
						HWSSettings.getMeasurement("JogSpeed"), HWSSettings.getMeasurement("SinkSpeed"));

				double skinThickness = HWSSettings.getMeasurement("SkinThickness");
				double frameThickness = HWSSettings.getMeasurement("FrameThickness");
				double webbing = HWSSettings.getMeasurement("Webbing");
				double tailOffset = HWSSettings.getMeasurement("TailOffset");
				double noseOffset = HWSSettings.getMeasurement("NoseOffset");
				double distanceToRail = HWSSettings.getMeasurement("DistanceFromRail");

				BezierBoardDrawUtil.printDeckSkinTemplate(gdraw, 0.0, 0.0, 1.0, 0.0, true,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail);

				settingsDialog.dispose();
			}

		};

		gcodeHWSMenu.add(gcodeHWSBottomTemplate);

		final AbstractAction gcodeHWSAllInternalTemplates = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GCODEHWSALLINTERNALTEMPLATES_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("HWSPARAMETERSCATEGORY_STR");
				Settings HWSSettings = settings.addCategory(categoryName);
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("SkinThickness", 0.4, LanguageResource.getString("HWSSKINTHICKNESS_STR"));
				HWSSettings.addMeasurement("FrameThickness", 0.5, LanguageResource.getString("HWSFRAMETHICKNESS_STR"));
				HWSSettings.addMeasurement("Webbing", 1.5, LanguageResource.getString("HWSWEBBING_STR"));
				HWSSettings.addMeasurement("NoseOffset", 3.5, LanguageResource.getString("HWSNOSEOFFSET_STR"));
				HWSSettings.addMeasurement("TailOffset", 3.5, LanguageResource.getString("HWSTAILOFFSET_STR"));
				HWSSettings.addMeasurement("DistanceFromRail", 3.0,
						LanguageResource.getString("HWSDISTANCEFROMRAIL_STR"));
				HWSSettings.addMeasurement("CutterDiam", 5.0, LanguageResource.getString("CUTTERDIAMETER_STR"));
				HWSSettings.addMeasurement("JogHeight", 5.0, LanguageResource.getString("JOGHEIGHT_STR"));
				HWSSettings.addMeasurement("JogSpeed", 20.0, LanguageResource.getString("JOGSPEED_STR"));
				HWSSettings.addMeasurement("SinkSpeed", 2.0, LanguageResource.getString("SINKSPEED_STR"));
				HWSSettings.addMeasurement("CutDepth", 0.0, LanguageResource.getString("CUTDEPTH_STR"));
				HWSSettings.addMeasurement("CutSpeed", 2.0, LanguageResource.getString("CUTSPEED_STR"));
				settings.getPreferences();
				String filename = FileTools.removeExtension(getCurrentBrd().getFilename());
				filename = filename.concat(" HWSFrame.nc");
				HWSSettings.addFileName("Filename", filename, LanguageResource.getString("FILENAME_STR"));
				HWSSettings.addMeasurement("OffsetX", 0.0, LanguageResource.getString("OFFSET_X_STR"));
				HWSSettings.addMeasurement("OffsetY", 0.0, LanguageResource.getString("OFFSET_Y_STR"));

				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("GCODEHWSALLINTERNALTEMPLATESTITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				if (settingsDialog.wasCancelled()) {
					settingsDialog.dispose();
					return;
				}
				settings.putPreferences();

				GCodeDraw gdraw = new GCodeDraw(HWSSettings.getFileName("Filename"),
						HWSSettings.getMeasurement("CutterDiam"), HWSSettings.getMeasurement("CutDepth"),
						HWSSettings.getMeasurement("CutSpeed"), HWSSettings.getMeasurement("JogHeight"),
						HWSSettings.getMeasurement("JogSpeed"), HWSSettings.getMeasurement("SinkSpeed"));

				gdraw.writeComment("HWS Frame");
				gdraw.writeComment(BoardCAD.getInstance().getCurrentBrd().getName() + " - "
						+ BoardCAD.getInstance().getCurrentBrd().getAuthor());

				double skinThickness = HWSSettings.getMeasurement("SkinThickness");
				double frameThickness = HWSSettings.getMeasurement("FrameThickness");
				double webbing = HWSSettings.getMeasurement("Webbing");
				double tailOffset = HWSSettings.getMeasurement("TailOffset");
				double noseOffset = HWSSettings.getMeasurement("NoseOffset");
				double distanceToRail = HWSSettings.getMeasurement("DistanceFromRail");
				double cutterDiam = HWSSettings.getMeasurement("CutterDiam");
				double offsetX = HWSSettings.getMeasurement("OffsetX");
				double offsetY = HWSSettings.getMeasurement("OffsetY");

				// Cut Stringer
				gdraw.writeComment("Stringer");
				gdraw.setFlipNormal(true);

				double x = offsetX;
				double y = offsetY;
				double scale = 1.0;

				/*
				 * PrintHollowWoodTemplates.printStringerWebbing(gdraw, x, y,
				 * scale, BoardCAD.getInstance().getCurrentBrd(), skinThickness,
				 * frameThickness, webbing);
				 *
				 * gdraw.setFlipNormal(false);
				 *
				 *
				 * BezierBoardDrawUtil.printProfile(gdraw, x, y, scale, false,
				 * BoardCAD.getInstance().getCurrentBrd(), 0.0, skinThickness,
				 * false, tailOffset, noseOffset);
				 *
				 * //Debug without offset gdraw.setCutterDiameter(0.0);
				 * BezierBoardDrawUtil.printProfile(gdraw, x, y, scale, false,
				 * BoardCAD.getInstance().getCurrentBrd(), 0.0, skinThickness,
				 * false, tailOffset, noseOffset);
				 *
				 *
				 * PrintHollowWoodTemplates.printStringerTailPieceCutOut(gdraw,
				 * x, y, scale, BoardCAD.getInstance().getCurrentBrd(),
				 * distanceToRail, skinThickness, frameThickness, webbing,
				 * tailOffset, noseOffset);
				 *
				 * PrintHollowWoodTemplates.printStringerNosePieceCutOut(gdraw,
				 * x, y, scale, BoardCAD.getInstance().getCurrentBrd(),
				 * distanceToRail, skinThickness, frameThickness, webbing,
				 * tailOffset, noseOffset);
				 *
				 * y+=
				 * BoardCAD.getInstance().getCurrentBrd().getThickness()*10.0;
				 * y+= cutterDiam*2.0;
				 */
				// Print rails twice
				gdraw.writeComment("Rails");

				BezierBoardDrawUtil.printRailTemplate(gdraw, x, y, scale, 0.0, false,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, tailOffset, noseOffset,
						false);

				PrintHollowWoodTemplates.printRailWebbing(gdraw, x, y, scale, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset);

				PrintHollowWoodTemplates.printRailNotching(gdraw, x, y, scale, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset);

				PrintHollowWoodTemplates.printRailNosePieceNotches(gdraw, x, y, scale, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset);

				PrintHollowWoodTemplates.printRailTailPieceNotches(gdraw, x, y, scale, 0.0,
						BoardCAD.getInstance().getCurrentBrd(), distanceToRail, skinThickness, frameThickness, webbing,
						tailOffset, noseOffset);
				settingsDialog.dispose();
				gdraw.close();
				return;
				/*
				 * x+= BoardCAD.getInstance().getCurrentBrd().getThickness();
				 * x+= cutterDiam*2.0;
				 *
				 * BezierBoardDrawUtil.printRailTemplate(gdraw, x, y, scale,
				 * false, BoardCAD.getInstance().getCurrentBrd(),
				 * distanceToRail, skinThickness, tailOffset, noseOffset,
				 * false);
				 *
				 * PrintHollowWoodTemplates.printRailWebbing(gdraw, x, y, scale,
				 * BoardCAD.getInstance().getCurrentBrd(), distanceToRail,
				 * skinThickness, frameThickness, webbing, tailOffset,
				 * noseOffset);
				 *
				 * PrintHollowWoodTemplates.printRailNotching(gdraw, x, y,
				 * scale, BoardCAD.getInstance().getCurrentBrd(),
				 * distanceToRail, skinThickness, frameThickness, webbing,
				 * tailOffset, noseOffset);
				 *
				 * PrintHollowWoodTemplates.printRailNosePieceNotches(gdraw, x,
				 * y, scale, BoardCAD.getInstance().getCurrentBrd(),
				 * distanceToRail, skinThickness, frameThickness, webbing,
				 * tailOffset, noseOffset);
				 *
				 * PrintHollowWoodTemplates.printRailTailPieceNotches(gdraw, x,
				 * y, scale, BoardCAD.getInstance().getCurrentBrd(),
				 * distanceToRail, skinThickness, frameThickness, webbing,
				 * tailOffset, noseOffset);
				 *
				 * //Print cross sections gdraw.writeComment("Ribs"); x+=
				 * BoardCAD.getInstance().getCurrentBrd().getThickness(); x+=
				 * cutterDiam*2.0; y=offsetY;
				 *
				 * int nrOfCrossSections =
				 * (int)((BoardCAD.getInstance().getCurrentBrd().getLength() -
				 * 9.0*UnitUtils.INCH)/UnitUtils.FOOT); double crosssectionPos =
				 * 0.0; for(int i = 0; i < nrOfCrossSections; i++) {
				 * crosssectionPos = (i+1)* UnitUtils.FOOT;
				 *
				 * gdraw.writeComment("Rib at " +
				 * UnitUtils.convertLengthToCurrentUnit(crosssectionPos, true));
				 *
				 * PrintHollowWoodTemplates.printCrossSection(gdraw, x, y,
				 * scale, BoardCAD.getInstance().getCurrentBrd(),
				 * crosssectionPos, distanceToRail, skinThickness,
				 * frameThickness, webbing, false);
				 *
				 * PrintHollowWoodTemplates.printCrossSectionWebbing(gdraw, x,
				 * y, scale, BoardCAD.getInstance().getCurrentBrd(),
				 * crosssectionPos, distanceToRail, skinThickness,
				 * frameThickness, webbing, false);
				 *
				 * double verticalStep =
				 * BoardCAD.getInstance().getCurrentBrd().getThicknessAtPos
				 * (crosssectionPos) - skinThickness + (cutterDiam*2.0);
				 *
				 * y += verticalStep; }
				 *
				 *
				 * //Nose piece gdraw.writeComment("Nose");
				 * PrintHollowWoodTemplates.printNosePiece(gdraw, x, y, scale,
				 * BoardCAD.getInstance().getCurrentBrd(), distanceToRail,
				 * skinThickness, frameThickness, webbing, tailOffset,
				 * noseOffset, false);
				 *
				 * PrintHollowWoodTemplates.printNosePiece(gdraw, x, y, scale,
				 * BoardCAD.getInstance().getCurrentBrd(), distanceToRail,
				 * skinThickness, frameThickness, webbing, tailOffset,
				 * noseOffset, true);
				 *
				 * PrintHollowWoodTemplates.printNosePieceWebbing(gdraw, x, y,
				 * scale, BoardCAD.getInstance().getCurrentBrd(),
				 * distanceToRail, skinThickness, frameThickness, webbing,
				 * tailOffset, noseOffset, false);
				 *
				 * PrintHollowWoodTemplates.printNosePieceWebbing(gdraw, x, y,
				 * scale, BoardCAD.getInstance().getCurrentBrd(),
				 * distanceToRail, skinThickness, frameThickness, webbing,
				 * tailOffset, noseOffset, true);
				 *
				 * //Tail piece gdraw.writeComment("Tail"); y += UnitUtils.FOOT;
				 * PrintHollowWoodTemplates.printTailPiece(gdraw, x, y, scale,
				 * BoardCAD.getInstance().getCurrentBrd(), distanceToRail,
				 * skinThickness, frameThickness, webbing, tailOffset,
				 * noseOffset, false);
				 *
				 * PrintHollowWoodTemplates.printTailPiece(gdraw, x, y, scale,
				 * BoardCAD.getInstance().getCurrentBrd(), distanceToRail,
				 * skinThickness, frameThickness, webbing, tailOffset,
				 * noseOffset, true);
				 *
				 * PrintHollowWoodTemplates.printTailPieceWebbing(gdraw, x, y,
				 * scale, BoardCAD.getInstance().getCurrentBrd(),
				 * distanceToRail, skinThickness, frameThickness, webbing,
				 * tailOffset, noseOffset, false);
				 *
				 * PrintHollowWoodTemplates.printTailPieceWebbing(gdraw, x, y,
				 * scale, BoardCAD.getInstance().getCurrentBrd(),
				 * distanceToRail, skinThickness, frameThickness, webbing,
				 * tailOffset, noseOffset, true);
				 *
				 *
				 * settingsDialog.dispose();
				 *
				 * gdraw.close();
				 */
			}

		};

		gcodeHWSMenu.add(gcodeHWSAllInternalTemplates);

		gcodeMenu.add(gcodeHWSMenu);

		fileMenu.add(gcodeMenu);

		final JMenu extensionsMenu = new JMenu(LanguageResource.getString("EXTENSIONSMENU_STR"));
		final JMenu atuaCoresMenu = new JMenu(LanguageResource.getString("ATUACORESMENU_STR"));
		final AbstractAction atuaCoresProfile = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("ATUACORESPROFILE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CategorizedSettings settings = new CategorizedSettings();
				String categoryName = LanguageResource.getString("ATUAPARAMETERSCATEGORY_STR");
				Settings atuaSettings = settings.addCategory(categoryName);
				atuaSettings.addBoolean("NoRotation", false, LanguageResource.getString("ATUANOROTATION_STR"));
				SettingDialog settingsDialog = new SettingDialog(settings);
				settingsDialog.setTitle(LanguageResource.getString("ATUAPARAMETERSTITLE_STR"));
				settingsDialog.setModal(true);
				settingsDialog.setVisible(true);
				settingsDialog.dispose();
				if (settingsDialog.wasCancelled()) {
					return;
				}

				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				filename = FileTools.setExtension(filename, "atua");

				BoardCAD.defaultDirectory = file.getPath();

				AtuaCoresToolpathGenerator toolpathGenerator = new AtuaCoresToolpathGenerator();
				try {
					toolpathGenerator.writeProfile(filename, getCurrentBrd(), atuaSettings.getBoolean("NoRotation"));
				} catch (Exception e) {
					String str = LanguageResource.getString("ATUACORESPROFILEFAILEDMSG_STR") + e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("ATUACORESPROFILEFAILEDTITLE_STR"), JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		atuaCoresMenu.add(atuaCoresProfile);

		final AbstractAction atuaCoresOutline = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("ATUACORESOUTLINE_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();

				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showSaveDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				filename = FileTools.setExtension(filename, "atua");

				BoardCAD.defaultDirectory = file.getPath();

				AtuaCoresToolpathGenerator toolpathGenerator = new AtuaCoresToolpathGenerator();
				try {
					toolpathGenerator.writeOutline(filename, getCurrentBrd());
				} catch (Exception e) {
					String str = LanguageResource.getString("ATUACORESOUTLINEFAILEDMSG_STR") + e.toString();
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(), str,
							LanguageResource.getString("ATUACORESOUTLINEFAILEDTITLE_STR"), JOptionPane.ERROR_MESSAGE);

				}
			}

		};
		atuaCoresMenu.add(atuaCoresOutline);

		extensionsMenu.add(atuaCoresMenu);

		fileMenu.add(extensionsMenu);

		fileMenu.addSeparator();

		final AbstractAction loadscript = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, "Load script");
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_I,
				// KeyEvent.CTRL_DOWN_MASK));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				final JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File(BoardCAD.defaultDirectory));

				int returnVal = fc.showOpenDialog(BoardCAD.getInstance().getFrame());
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				String filename = file.getPath(); // Load and display
				// selection
				if (filename == null)
					return;

				// ScriptLoader sl=new ScriptLoader();
				// sl.loadScript(filename);

				BoardCAD.defaultDirectory = file.getPath();

			}

		};
		fileMenu.add(loadscript);

		fileMenu.addSeparator();

		final AbstractAction test = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, "Test");
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				/*
				 * CategorizedSettings settings = new CategorizedSettings();
				 * settings.addCategory("test");
				 * settings.getSettings("test").addBoolean("Test1", true,
				 * "Test 1");
				 *
				 * SettingDialog dialog = new SettingDialog(settings);
				 * dialog.setModal(true); dialog.setVisible(true);
				 * if(!dialog.wasCancelled()) { boolean test1 =
				 * settings.getSettings("test").getBoolean("Test1");
				 * System.out.printf("Test1: %s", test1?"true":"false"); }
				 *
				 * BezierSpline b =
				 * getCurrentBrd().getNearestCrossSection(getCurrentBrd
				 * ().getLength()/2.0f).getBezierSpline(); double startAngle =
				 * b.getNormalByS(BezierSpline.ZERO); double endAngle =
				 * b.getNormalByS(BezierSpline.ONE);
				 *
				 * System.out.printf("startAngle: %f endAngle: %f\n",
				 * startAngle/BezierBoard.DEG_TO_RAD,
				 * endAngle/BezierBoard.DEG_TO_RAD);
				 *
				 * int steps = 20;
				 *
				 * System.out.printf(
				 * "----------------------------------------\n" );
				 * System.out.printf(
				 * "----------------------------------------\n" ); System.out.
				 * printf("---------------TEST BEGIN---------------\n" );
				 * System.out.printf(
				 * "----------------------------------------\n" );
				 *
				 * for(int i = 0; i < steps; i++) { double currentAngle =
				 * b.getNormalByS((double)i/(double)steps);
				 * System.out.printf("Angle:%f\n",
				 * currentAngle/BezierBoard.DEG_TO_RAD); }
				 *
				 * System.out.printf(
				 * "----------------------------------------\n" );
				 * System.out.printf(
				 * "----------------------------------------\n" );
				 *
				 * double angleStep = (endAngle-startAngle) / steps;
				 *
				 * for(int i = 0; i < steps; i++) { System.out.printf(
				 * "----------------------------------------\n" ); double
				 * currentAngle = startAngle + (angleStep*i); double s =
				 * b.getSByNormalReverse(currentAngle); double checkAngle =
				 * b.getNormalByS(s); System.out.
				 * printf("Target Angle:%f Result s:%f Angle for s:%f\n" ,
				 * currentAngle/BezierBoard.DEG_TO_RAD, s,
				 * checkAngle/BezierBoard.DEG_TO_RAD); }
				 */

				System.out.printf("__________________________________\n");
				// Test SimpleBullnoseCutter
				SimpleBullnoseCutter cutter = new SimpleBullnoseCutter(50, 10, 100);
				System.out.printf("TEST!!! Cutter diam: 50 corner: 10 height: 100\n");

				Point3d point = new Point3d(0.0, 0.0, 0.0);

				Vector<Vector3d> testVectors = new Vector<Vector3d>();

				testVectors.add(new Vector3d(1.0, 1.0, 1.0));
				// testVectors.add(new Vector3d(1.0,0.0,1.0));
				// testVectors.add(new Vector3d(-1.0,0.0,1.0));
				// testVectors.add(new Vector3d(0.0,1.0,1.0));
				// testVectors.add(new Vector3d(0.0,-1.0,1.0));
				// testVectors.add(new Vector3d(0.0,-1.0,0.0));
				// testVectors.add(new Vector3d(1.0,0.0,1.0));

				System.out.printf("\n__________________________________\n");
				for (int i = 0; i < testVectors.size(); i++) {
					Vector3d vector = testVectors.elementAt(i);
					vector.normalize();
					System.out.printf("\nTEST!!! Vector%d: %f,%f,%f\n", i, vector.x, vector.y, vector.z);
					double[] result = cutter.calcOffset(point, vector, null);
					System.out.printf("Result: %f, %f, %f\n", result[0], result[1], result[2]);
				}
				System.out.printf("\n__________________________________\n");

			}
		};
		fileMenu.add(test);

		final AbstractAction exit = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("EXIT_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				mFrame.dispose();
				// mFrame.setVisible(false);
			}

		};
		fileMenu.add(exit);

		menuBar.add(fileMenu);

		final JMenu editMenu = new JMenu(LanguageResource.getString("EDITMENU_STR"));
		editMenu.setMnemonic(KeyEvent.VK_E);
		final AbstractAction undo = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("UNDO_STR"));
				this.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("UNDO_STR"));
				this.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/edit-undo.png")));
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				BrdCommandHistory.getInstance().undo();
				mFrame.repaint();
			}

		};
		editMenu.add(undo);

		final AbstractAction redo = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("REDO_STR"));
				this.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("REDO_STR"));
				this.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/edit-redo.png")));

				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				BrdCommandHistory.getInstance().redo();
				mFrame.repaint();
			}

		};
		editMenu.add(redo);

		menuBar.add(editMenu);

		final JMenu viewMenu = new JMenu(LanguageResource.getString("VIEWMENU_STR"));
		viewMenu.setMnemonic(KeyEvent.VK_V);

		mIsPaintingGridMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWGRID_STR"));
		mIsPaintingGridMenuItem.setMnemonic(KeyEvent.VK_R);
		mIsPaintingGridMenuItem.setSelected(true);
		mIsPaintingGridMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingGridMenuItem);

		mIsPaintingGhostBrdMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWGHOSTBOARD_STR"));
		mIsPaintingGhostBrdMenuItem.setMnemonic(KeyEvent.VK_G);
		mIsPaintingGhostBrdMenuItem.setSelected(true);
		mIsPaintingGhostBrdMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingGhostBrdMenuItem);

		mIsPaintingOriginalBrdMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWORIGINALBOARD_STR"));
		mIsPaintingOriginalBrdMenuItem.setMnemonic(KeyEvent.VK_O);
		mIsPaintingOriginalBrdMenuItem.setSelected(true);
		mIsPaintingOriginalBrdMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingOriginalBrdMenuItem);

		mIsPaintingControlPointsMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWCONTROLPOINTS_STR"));
		mIsPaintingControlPointsMenuItem.setMnemonic(KeyEvent.VK_C);
		mIsPaintingControlPointsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0));// ,
																									// KeyEvent.CTRL_DOWN_MASK));
		mIsPaintingControlPointsMenuItem.setSelected(true);
		mIsPaintingControlPointsMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingControlPointsMenuItem);

		mIsPaintingNonActiveCrossSectionsMenuItem = new JCheckBoxMenuItem(
				LanguageResource.getString("SHOWNONEACTIVECROSSECTIONS_STR"));
		mIsPaintingNonActiveCrossSectionsMenuItem.setMnemonic(KeyEvent.VK_N);
		mIsPaintingNonActiveCrossSectionsMenuItem.setSelected(true);
		mIsPaintingNonActiveCrossSectionsMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingNonActiveCrossSectionsMenuItem);

		mIsPaintingGuidePointsMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWGUIDEPOINTS_STR"));
		mIsPaintingGuidePointsMenuItem.setMnemonic(KeyEvent.VK_P);
		mIsPaintingGuidePointsMenuItem.setSelected(true);
		mIsPaintingGuidePointsMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingGuidePointsMenuItem);

		mIsPaintingCurvatureMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWCURVATURE_STR"));
		mIsPaintingCurvatureMenuItem.setMnemonic(KeyEvent.VK_V);
		mIsPaintingCurvatureMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));// ,
																								// KeyEvent.CTRL_DOWN_MASK));
		mIsPaintingCurvatureMenuItem.setSelected(true);
		mIsPaintingCurvatureMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingCurvatureMenuItem);

		mIsPaintingVolumeDistributionMenuItem = new JCheckBoxMenuItem(
				LanguageResource.getString("SHOWVOLUMEDISTRIBUTION_STR"));
		mIsPaintingVolumeDistributionMenuItem.setMnemonic(KeyEvent.VK_V);
		mIsPaintingVolumeDistributionMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0));// ,
																										// KeyEvent.CTRL_DOWN_MASK));
		mIsPaintingVolumeDistributionMenuItem.setSelected(true);
		mIsPaintingVolumeDistributionMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingVolumeDistributionMenuItem);

		mIsPaintingCenterOfMassMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWCENTEROFMASS_STR"));
		mIsPaintingCenterOfMassMenuItem.setMnemonic(KeyEvent.VK_M);
		mIsPaintingCenterOfMassMenuItem.setSelected(true);
		mIsPaintingCenterOfMassMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingCenterOfMassMenuItem);

		mIsPaintingSlidingInfoMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWSLIDINGINFO_STR"));
		mIsPaintingSlidingInfoMenuItem.setMnemonic(KeyEvent.VK_S);
		mIsPaintingSlidingInfoMenuItem.setSelected(true);
		mIsPaintingSlidingInfoMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingSlidingInfoMenuItem);

		mIsPaintingSlidingCrossSectionMenuItem = new JCheckBoxMenuItem(
				LanguageResource.getString("SHOWSLIDINGCROSSECTION_STR"));
		mIsPaintingSlidingCrossSectionMenuItem.setMnemonic(KeyEvent.VK_X);
		mIsPaintingSlidingCrossSectionMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));// ,
																										// KeyEvent.CTRL_DOWN_MASK));
		mIsPaintingSlidingCrossSectionMenuItem.setSelected(true);
		mIsPaintingSlidingCrossSectionMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingSlidingCrossSectionMenuItem);

		mIsPaintingFinsMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWFINS_STR"));
		mIsPaintingFinsMenuItem.setMnemonic(KeyEvent.VK_F);
		mIsPaintingFinsMenuItem.setSelected(true);
		mIsPaintingFinsMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingFinsMenuItem);

		mIsPaintingBackgroundImageMenuItem = new JCheckBoxMenuItem(
				LanguageResource.getString("SHOWBACKGROUNDIMAGE_STR"));
		mIsPaintingBackgroundImageMenuItem.setMnemonic(KeyEvent.VK_B);
		mIsPaintingBackgroundImageMenuItem.setSelected(true);
		mIsPaintingBackgroundImageMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingBackgroundImageMenuItem);

		mIsAntialiasingMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("USEANTIALIASING_STR"));
		mIsAntialiasingMenuItem.setMnemonic(KeyEvent.VK_A);
		mIsAntialiasingMenuItem.setSelected(true);
		mIsAntialiasingMenuItem.addItemListener(this);
		viewMenu.add(mIsAntialiasingMenuItem);

		mIsPaintingBaseLineMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWBASELINE_STR"));
		mIsPaintingBaseLineMenuItem.setMnemonic(KeyEvent.VK_L);
		mIsPaintingBaseLineMenuItem.setSelected(true);
		mIsPaintingBaseLineMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingBaseLineMenuItem);

		mIsPaintingCenterLineMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWCENTERLINE_STR"));
		mIsPaintingCenterLineMenuItem.setMnemonic(KeyEvent.VK_J);
		mIsPaintingCenterLineMenuItem.setSelected(true);
		mIsPaintingCenterLineMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingCenterLineMenuItem);

		mIsPaintingOverCurveMesurementsMenuItem = new JCheckBoxMenuItem(
				LanguageResource.getString("SHOWOVERBOTTOMCURVEMEASUREMENTS_STR"));
		mIsPaintingOverCurveMesurementsMenuItem.setMnemonic(KeyEvent.VK_D);
		mIsPaintingOverCurveMesurementsMenuItem.setAccelerator(KeyStroke.getKeyStroke("shift V"));
		mIsPaintingOverCurveMesurementsMenuItem.setSelected(true);
		mIsPaintingOverCurveMesurementsMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingOverCurveMesurementsMenuItem);

		mIsPaintingMomentOfInertiaMenuItem = new JCheckBoxMenuItem(
				LanguageResource.getString("SHOWMOMENTOFINERTIA_STR"));
		mIsPaintingMomentOfInertiaMenuItem.setMnemonic(KeyEvent.VK_D);
		// mIsPaintingMomentOfInertiaMenuItem.setAccelerator(
		// KeyStroke.getKeyStroke("shift V") );
		mIsPaintingMomentOfInertiaMenuItem.setSelected(true);
		mIsPaintingMomentOfInertiaMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingMomentOfInertiaMenuItem);

		mIsPaintingCrossectionsPositionsMenuItem = new JCheckBoxMenuItem(
				LanguageResource.getString("SHOWCROSSECTIONSPOSITIONS_STR"));
		mIsPaintingCrossectionsPositionsMenuItem.setMnemonic(KeyEvent.VK_D);
		// mIsPaintingCrossectionsPositionsMenuItem.setAccelerator(
		// KeyStroke.getKeyStroke("shift V") );
		mIsPaintingCrossectionsPositionsMenuItem.setSelected(true);
		mIsPaintingCrossectionsPositionsMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingCrossectionsPositionsMenuItem);

		mIsPaintingFlowlinesMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWFLOWLINES_STR"));
		mIsPaintingFlowlinesMenuItem.setMnemonic(KeyEvent.VK_D);
		// mIsPaintingFlowlinesMenuItem.setAccelerator(
		// KeyStroke.getKeyStroke("shift V") );
		mIsPaintingFlowlinesMenuItem.setSelected(true);
		mIsPaintingFlowlinesMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingFlowlinesMenuItem);

		mIsPaintingApexlineMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWAPEXLINE_STR"));
		mIsPaintingApexlineMenuItem.setMnemonic(KeyEvent.VK_D);
		// mIsPaintingApexlineMenuItem.setAccelerator(
		// KeyStroke.getKeyStroke("shift V") );
		mIsPaintingApexlineMenuItem.setSelected(true);
		mIsPaintingApexlineMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingApexlineMenuItem);

		mIsPaintingTuckUnderLineMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWTUCKUNDERLINE_STR"));
		mIsPaintingTuckUnderLineMenuItem.setMnemonic(KeyEvent.VK_D);
		// mIsPaintingTuckUnderLineMenuItem.setAccelerator(
		// KeyStroke.getKeyStroke("shift V") );
		mIsPaintingTuckUnderLineMenuItem.setSelected(true);
		mIsPaintingTuckUnderLineMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingTuckUnderLineMenuItem);

		mIsPaintingFootMarksMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWFOOTMARKS_STR"));
		mIsPaintingFootMarksMenuItem.setMnemonic(KeyEvent.VK_D);
		mIsPaintingFootMarksMenuItem.setAccelerator(KeyStroke.getKeyStroke("shift F"));
		mIsPaintingFootMarksMenuItem.setSelected(false);
		mIsPaintingFootMarksMenuItem.addItemListener(this);
		viewMenu.add(mIsPaintingFootMarksMenuItem);

		mUseFillMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("USEFILL_STR"));
		// mUseFillMenuItem.setMnemonic(KeyEvent.VK_D);
		// mUseFillMenuItem.setAccelerator(KeyStroke.getKeyStroke("shift F") );
		mUseFillMenuItem.setSelected(true);
		mUseFillMenuItem.addItemListener(this);
		viewMenu.add(mUseFillMenuItem);

		menuBar.add(viewMenu);

		final JMenu crossSectionsMenu = new JMenu(LanguageResource.getString("CROSSECTIONSMENU_STR"));
		crossSectionsMenu.setMnemonic(KeyEvent.VK_C);

		mNextCrossSection = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("NEXTCROSSECTION_STR"));
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (isGhostMode()) {
					BoardCAD.getInstance().getGhostBrd().nextCrossSection();
				} else if (mOrgFocus) {
					BoardCAD.getInstance().getOriginalBrd().nextCrossSection();
				} else {
					BoardCAD.getInstance().getCurrentBrd().nextCrossSection();
				}
				mFrame.repaint();
			}

		};
		crossSectionsMenu.add(mNextCrossSection);

		mPreviousCrossSection = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PREVIOUSCROSSECTION_STR"));
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (isGhostMode()) {
					BoardCAD.getInstance().getGhostBrd().previousCrossSection();
				} else if (mOrgFocus) {
					BoardCAD.getInstance().getOriginalBrd().previousCrossSection();
				} else {
					BoardCAD.getInstance().getCurrentBrd().previousCrossSection();
				}
				mFrame.repaint();
			}

		};
		crossSectionsMenu.add(mPreviousCrossSection);
		crossSectionsMenu.addSeparator();

		final AbstractAction addCrossSection = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("ADDCROSSECTION_STR"));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setSelectedEdit(mCrossSectionEdit);

				double pos = 0.0f;
				String posStr = JOptionPane.showInputDialog(mFrame, LanguageResource.getString("ADDCROSSECTIONMSG_STR"),
						LanguageResource.getString("ADDCROSSECTIONTITLE_STR"), JOptionPane.PLAIN_MESSAGE);

				if (posStr == null)
					return;

				pos = UnitUtils.convertInputStringToInternalLengthUnit(posStr);
				if (pos <= 0 || pos > getCurrentBrd().getLength()) {
					JOptionPane.showMessageDialog(getFrame(),
							LanguageResource.getString("ADDCROSSECTIONPOSITIONINVALIDMSG_STR"),
							LanguageResource.getString("ADDCROSSECTIONPOSITIONINVALIDTITLE_STR"),
							JOptionPane.WARNING_MESSAGE);
					return;
				}

				BrdAddCrossSectionCommand cmd = new BrdAddCrossSectionCommand(mCrossSectionEdit, pos);
				cmd.execute();

				mFrame.repaint();
			}

		};
		crossSectionsMenu.add(addCrossSection);

		final AbstractAction moveCrossSection = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("MOVECROSSECTION_STR"));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setSelectedEdit(mCrossSectionEdit);

				double pos = 0.0f;
				String posStr = JOptionPane.showInputDialog(mFrame,
						LanguageResource.getString("MOVECROSSECTIONMSG_STR"),
						LanguageResource.getString("MOVECROSSECTIONTITLE_STR"), JOptionPane.PLAIN_MESSAGE);

				if (posStr == null)
					return;

				pos = UnitUtils.convertInputStringToInternalLengthUnit(posStr);
				if (pos <= 0 || pos > getCurrentBrd().getLength()) {
					JOptionPane.showMessageDialog(getFrame(),
							LanguageResource.getString("MOVECROSSECTIONPOSITIONINVALIDMSG_STR"),
							LanguageResource.getString("MOVECROSSECTIONPOSITIONINVALIDTITLE_STR"),
							JOptionPane.WARNING_MESSAGE);
					return;
				}

				BrdMoveCrossSectionCommand cmd = new BrdMoveCrossSectionCommand(mCrossSectionEdit,
						mCrossSectionEdit.getCurrentBrd().getCurrentCrossSection(), pos);
				cmd.execute();

				mFrame.repaint();
			}

		};
		crossSectionsMenu.add(moveCrossSection);

		final AbstractAction deleteCrossSection = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("REMOVECROSSECTION_STR"));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setSelectedEdit(mCrossSectionEdit);

				if (mCrossSectionEdit.getCurrentBrd().getCrossSections().size() <= 3) {
					JOptionPane.showMessageDialog(getFrame(),
							LanguageResource.getString("REMOVECROSSECTIONDELETELASTERRORMSG_STR"),
							LanguageResource.getString("REMOVECROSSECTIONDELETELASTERRORTITLE_STR"),
							JOptionPane.WARNING_MESSAGE);

					return;
				}

				BrdRemoveCrossSectionCommand cmd = new BrdRemoveCrossSectionCommand(mCrossSectionEdit,
						mCrossSectionEdit.getCurrentBrd().getCurrentCrossSection());
				cmd.execute();

				mFrame.repaint();
			}

		};
		crossSectionsMenu.add(deleteCrossSection);
		crossSectionsMenu.addSeparator();

		final AbstractAction copyCrossSection = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("COPYCROSSECTION_STR"));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				mCrossSectionCopy = (BezierBoardCrossSection) getCurrentBrd().getCurrentCrossSection().clone();

				mFrame.repaint();
			}

		};
		crossSectionsMenu.add(copyCrossSection);

		final AbstractAction pasteCrossSection = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("PASTECROSSECTION_STR"));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (mCrossSectionCopy == null)
					return;

				setSelectedEdit(mCrossSectionEdit);

				BrdPasteCrossSectionCommand cmd = new BrdPasteCrossSectionCommand(mCrossSectionEdit,
						getCurrentBrd().getCurrentCrossSection(), mCrossSectionCopy);
				cmd.execute();

				mFrame.repaint();
			}

		};
		crossSectionsMenu.add(pasteCrossSection);

		menuBar.add(crossSectionsMenu);

		final JMenu boardMenu = new JMenu(LanguageResource.getString("BOARDMENU_STR"));
		boardMenu.setMnemonic(KeyEvent.VK_B);

		final AbstractAction scale = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("SCALECURRENT_STR"));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				BrdScaleCommand cmd = new BrdScaleCommand(getSelectedEdit());
				cmd.execute();

				mFrame.repaint();
			}

		};
		boardMenu.add(scale);

		final AbstractAction scaleGhost = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("SCALEGHOST_STR"));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				getGhostBrd().scale(getCurrentBrd().getLength(), getCurrentBrd().getCenterWidth(),
						getCurrentBrd().getThickness());

				mFrame.repaint();
			}

		};
		boardMenu.add(scaleGhost);

		final AbstractAction info = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("INFO_STR"));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				BoardInfo dialog = new BoardInfo(getCurrentBrd());
				dialog.setModal(true);
				// dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				dialog.setVisible(true);
				dialog.dispose();
				mFrame.repaint();
			}

		};
		boardMenu.addSeparator();
		boardMenu.add(info);

		final AbstractAction fins = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("FINS_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				BoardFinsDialog dialog = new BoardFinsDialog(getCurrentBrd());
				dialog.setModal(true);
				// dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				dialog.setVisible(true);
				dialog.dispose();
				mFrame.repaint();
			}

		};
		boardMenu.add(fins);

		final AbstractAction guidePoints = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("GUIDEPOINTS_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				mGuidePointsDialog.setVisible(true);
				mFrame.repaint();
			}

		};
		boardMenu.addSeparator();
		boardMenu.add(guidePoints);

		final AbstractAction weightCalc = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("WEIGHTCALC_STR"));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				mWeightCalculatorDialog.setDefaults();
				mWeightCalculatorDialog.updateAll();
				mWeightCalculatorDialog.setVisible(true);
			}

		};
		boardMenu.addSeparator();
		boardMenu.add(weightCalc);

		final AbstractAction flip = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("FLIP_STR"));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {

				mOutlineEdit.setFlipped(!mOutlineEdit.isFlipped());
				if (mOutlineEdit2 != null)
					mOutlineEdit2.setFlipped(!mOutlineEdit2.isFlipped());
				mBottomAndDeckEdit.setFlipped(!mBottomAndDeckEdit.isFlipped());
				mCrossSectionOutlineEdit.setFlipped(!mCrossSectionOutlineEdit.isFlipped());

				mQuadViewOutlineEdit.setFlipped(!mQuadViewOutlineEdit.isFlipped());
				mQuadViewRockerEdit.setFlipped(!mQuadViewCrossSectionEdit.isFlipped());

				fitAll();

				mFrame.repaint();
			}

		};
		boardMenu.addSeparator();
		boardMenu.add(flip);

		menuBar.add(boardMenu);

		final JMenu miscMenu = new JMenu(LanguageResource.getString("MISCMENU_STR"));
		miscMenu.setMnemonic(KeyEvent.VK_M);

		final AbstractAction settings = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("SETTINGS_STR"));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				BoardCADSettingsDialog dlg = new BoardCADSettingsDialog(mSettings);
				dlg.setModal(true);
				dlg.setVisible(true);

				mFrame.repaint();
			}

		};

		miscMenu.add(settings);

		final AbstractAction language = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("LANGUAGE_STR"));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				ComboBoxDialog languageDlg = new ComboBoxDialog(mFrame);
				languageDlg.setTitle(LanguageResource.getString("LANGUAGE_STR"));
				languageDlg.setMessageText(LanguageResource.getString("SELECTLANGUAGE_STR"));

				String[] languages = new String[mSupportedLanguages.length];
				for (int i = 0; i < mSupportedLanguages.length; i++) {
					languages[i] = mSupportedLanguages[i].getDisplayName();
				}

				languageDlg.setItems(languages);

				final Preferences prefs = Preferences.userNodeForPackage(BoardCAD.class);
				String languageStr = prefs.get("Language", "en");
				String selectedLanguage = "English";
				int i;
				for (i = 0; i < mSupportedLanguages.length; i++) {
					if (mSupportedLanguages[i].getLanguage().equals(languageStr)) {
						selectedLanguage = mSupportedLanguages[i].getDisplayName();
						break;
					}
				}
				languageDlg.setSelectedItem(selectedLanguage);

				languageDlg.setModal(true);

				languageDlg.setVisible(true);
				if (!languageDlg.wasCancelled()) {
					selectedLanguage = languageDlg.getSelectedItem();

					for (i = 0; i < mSupportedLanguages.length; i++) {
						if (mSupportedLanguages[i].getDisplayName().equals(selectedLanguage)) {
							languageStr = mSupportedLanguages[i].getLanguage();
							break;
						}
					}

					prefs.put("Language", languageStr);

					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(),
							LanguageResource.getString("LANGUAGECHANGEDMSG_STR"),
							LanguageResource.getString("LANGUAGECHANGEDTITLE_STR"), JOptionPane.INFORMATION_MESSAGE);
				}

				languageDlg.dispose();
				mFrame.repaint();
			}

		};
		miscMenu.add(language);

		miscMenu.addSeparator();

		final JMenu crossSectionInterpolationMenu = new JMenu(
				LanguageResource.getString("CROSSECTIONINTERPOLATIONMENU_STR"));
		mControlPointInterpolationButton = new JRadioButtonMenuItem(
				LanguageResource.getString("CROSSECTIONINTERPOLATIONTYPECONTROLPOINT_STR"));
		mSBlendInterpolationButton = new JRadioButtonMenuItem(
				LanguageResource.getString("CROSSECTIONINTERPOLATIONTYPESBLEND_STR"));

		ActionListener interpolationTypeListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setBoardChangedFor3D();
				boolean selected = mShowBezier3DModelMenuItem.getModel().isSelected();
				if (selected) {
					updateBezier3DModel();
				}
				if (mCurrentBrd != null) {
					mCurrentBrd.setInterpolationType(getCrossSectionInterpolationType());
				}
			}
		};

		mControlPointInterpolationButton.addActionListener(interpolationTypeListener);
		mSBlendInterpolationButton.addActionListener(interpolationTypeListener);

		final ButtonGroup interpolationButtonGroup = new ButtonGroup();
		interpolationButtonGroup.add(mControlPointInterpolationButton);
		interpolationButtonGroup.add(mSBlendInterpolationButton);

		crossSectionInterpolationMenu.add(mControlPointInterpolationButton);
		crossSectionInterpolationMenu.add(mSBlendInterpolationButton);

		miscMenu.add(crossSectionInterpolationMenu);

		menuBar.add(miscMenu);

		final JMenu menu3D = new JMenu(LanguageResource.getString("3DMODELMENU_STR"));
		menu3D.setMnemonic(KeyEvent.VK_D);

		menuBar.add(menu3D);

		final JMenu menuRender = new JMenu(LanguageResource.getString("RENDERMENU_STR"));
		menuRender.setMnemonic(KeyEvent.VK_R);

		mShowRenderInwireframe = new JCheckBoxMenuItem(LanguageResource.getString("SHOWWIREFRAME_STR"));
		mShowRenderInwireframe.setMnemonic(KeyEvent.VK_S);
		mShowRenderInwireframe.setSelected(false);
		mShowRenderInwireframe.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(final ItemEvent e) {
				Appearance a = new Appearance();
				PolygonAttributes pa = new PolygonAttributes();
				if (mShowRenderInwireframe.isSelected()) {
					Color3f ambient = new Color3f(0.1f, 0.5f, 0.1f);
					Color3f emissive = new Color3f(0.0f, 0.0f, 0.0f);
					Color3f diffuse = new Color3f(0.1f, 1.0f, 0.1f);
					Color3f specular = new Color3f(0.9f, 1.0f, 0.9f);

					// Set up the material properties
					a.setMaterial(new Material(ambient, emissive, diffuse, specular, 115.0f));

					pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
					pa.setCullFace(PolygonAttributes.CULL_BACK); // experiment
																	// with it
					a.setPolygonAttributes(pa);
				} else {
					Color3f ambient = new Color3f(0.4f, 0.4f, 0.45f);
					Color3f emissive = new Color3f(0.0f, 0.0f, 0.0f);
					Color3f diffuse = new Color3f(0.8f, 0.8f, 0.8f);
					Color3f specular = new Color3f(1.0f, 1.0f, 1.0f);

					// Set up the material properties
					a.setMaterial(new Material(ambient, emissive, diffuse, specular, 115.0f));

					pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
					pa.setCullFace(PolygonAttributes.CULL_BACK); // experiment
																	// with it
					a.setPolygonAttributes(pa);
				}

				if (mRendered3DView.getBezier3DModel() != null) {
					mRendered3DView.getBezier3DModel().setAppearance(a);
				}

				if (mQuad3DView.getBezier3DModel() != null) {
					mQuad3DView.getBezier3DModel().setAppearance(a);
				}

			}

		});
		menuRender.add(mShowRenderInwireframe);

		mShowBezier3DModelMenuItem = new JCheckBoxMenuItem(LanguageResource.getString("SHOWBEZIER3DMODEL_STR"));
		mShowBezier3DModelMenuItem.setMnemonic(KeyEvent.VK_B);
		mShowBezier3DModelMenuItem.setSelected(true);
		ActionListener showBezier3DListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				boolean selected = mShowBezier3DModelMenuItem.getModel().isSelected();
				mRendered3DView.setShowBezierModel(selected);
				mQuad3DView.setShowBezierModel(selected);
				if (selected) {
					updateBezier3DModel();
				}
			}
		};
		mShowBezier3DModelMenuItem.addActionListener(showBezier3DListener);
		menuRender.add(mShowBezier3DModelMenuItem);

		menuBar.add(menuRender);

		final JMenu helpMenu = new JMenu(LanguageResource.getString("HELPMENU_STR"));
		helpMenu.setMnemonic(KeyEvent.VK_H);

		final AbstractAction onlineHelp = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("ONLINEHELP_STR"));
				this.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/Help16.gif")));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				BrowserControl.displayURL("http://boardcad.org/index.php/Help:Contents");
			}

		};
		helpMenu.add(onlineHelp);

		final AbstractAction about = new AbstractAction() {
			static final long serialVersionUID = 1L;
			{
				this.putValue(Action.NAME, LanguageResource.getString("ABOUT_STR"));
				this.putValue(Action.SMALL_ICON,
						new ImageIcon(getClass().getResource("../../icons/Information16.gif")));
				// this.putValue(Action.ACCELERATOR_KEY,
				// KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
			};

			@Override
			public void actionPerformed(ActionEvent arg0) {
				AboutBox box = new AboutBox();
				box.setModal(true);
				box.setVisible(true);
				box.dispose();
			}

		};
		helpMenu.add(about);

		menuBar.add(helpMenu);

		mFrame.setJMenuBar(menuBar);

		mToolBar = new JToolBar();

		newBrd.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/new.png")));
		mToolBar.add(newBrd);

		loadBrd.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/open.png")));
		mToolBar.add(loadBrd);

		saveBrd.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/save.png")));
		mToolBar.add(saveBrd);

		SaveBrd.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/save-refresh.png")));
		mToolBar.add(SaveBrd);

		printSpecSheet.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/print.png")));
		mToolBar.add(printSpecSheet);

		mToolBar.addSeparator();
		mToolBar.addSeparator();

		final SetCurrentOneShotCommandAction zoom = new SetCurrentOneShotCommandAction(new BrdZoomCommand());
		zoom.putValue(Action.NAME, LanguageResource.getString("ZOOMBUTTON_STR"));
		zoom.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("ZOOMBUTTON_STR"));
		zoom.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/zoom-in.png")));
		mToolBar.add(zoom);

		final AbstractAction fit = new AbstractAction() {
			static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				fitAll();
				mLifeSizeButton.getModel().setPressed(false);
				mTabbedPane.repaint();
			}

		};
		fit.putValue(Action.NAME, LanguageResource.getString("FITBUTTON_STR"));
		fit.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("FITBUTTON_STR"));
		fit.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/zoom-fit-best.png")));
		mToolBar.add(fit);
		popupMenu.add(fit);

		mLifeSizeButton = new JToggleButton();
		mLifeSizeButton.setIcon(new ImageIcon(getClass().getResource("../../icons/zoom-1to1.png")));

		mLifeSizeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getID() == MouseEvent.MOUSE_CLICKED && e.getButton() == MouseEvent.BUTTON3) {
					BoardEdit edit = getSelectedEdit();
					if (edit != null) {
						edit.setCurrentAsLifeSizeScale();
					}
				}
			}

		});

		final ChangeListener lifeSizeChangeListner = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				boolean isLifeSize = mLifeSizeButton.isSelected();
				BoardEdit edit = getSelectedEdit();
				if (edit != null) {
					if (edit.isLifeSize() == isLifeSize)
						return;

					edit.setLifeSize(isLifeSize);

					if (!isLifeSize)
						edit.resetToPreviousScale();

					edit.repaint();
				}

			}
		};

		mLifeSizeButton.addChangeListener(lifeSizeChangeListner);
		// lifeSize.putValue(AbstractAction.SHORT_DESCRIPTION,
		// LanguageResource.getString("LIFESIZEBUTTON_STR"));
		// lifeSize.putValue(AbstractAction.SMALL_ICON, new
		// ImageIcon(getClass().getResource("../../icons/zoom-fit-best.png")));
		mToolBar.add(mLifeSizeButton);

		mToolBar.addSeparator();
		mToolBar.addSeparator();

		final SetCurrentCommandAction edit = new SetCurrentCommandAction(new BrdEditCommand());
		edit.putValue(Action.NAME, LanguageResource.getString("EDITBUTTON_STR"));
		edit.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("EDITBUTTON_STR"));
		edit.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/BoardCADedit24.gif")));
		mToolBar.add(edit);
		// popupMenu.add(edit);

		// JButton bt = new JButton(new ImageIcon("../../icons/Zoom24.gif"));
		// mToolBar.add(bt);

		final SetCurrentCommandAction pan = new SetCurrentCommandAction(new BrdPanCommand());
		pan.putValue(Action.NAME, LanguageResource.getString("PANBUTTON_STR"));
		pan.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("PANBUTTON_STR"));
		pan.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("../../icons/BoardCADpan24.gif")));

		mToolBar.add(pan);

		mToolBar.addSeparator();

		popupMenu.addSeparator();

		final JButton spotCheck = new JButton();
		final JButton spotCheck2 = new JButton();

		spotCheck.setText(LanguageResource.getString("SPOTCHECKBUTTON_STR"));
		spotCheck2.setText(LanguageResource.getString("SPOTCHECKBUTTON_STR"));

		final ChangeListener spotCheckChangeListner = new ChangeListener() {
			BrdSpotCheckCommand cmd = new BrdSpotCheckCommand();

			boolean mIsSpotChecking = false;

			@Override
			public void stateChanged(ChangeEvent e) {
				ButtonModel model = ((JButton) e.getSource()).getModel();
				if (model.isPressed()) {
					cmd.spotCheck();
					mIsSpotChecking = true;
				} else if (mIsSpotChecking == true) {
					cmd.restore();
					mIsSpotChecking = false;
				}

			}
		};

		spotCheck.addChangeListener(spotCheckChangeListner);
		spotCheck2.addChangeListener(spotCheckChangeListner);

		// mToolBar.add(spotCheck);
		popupMenu.add(spotCheck2);

		mToolBar.add(undo);
		mToolBar.add(redo);

		mToolBar.addSeparator();

		final SetCurrentCommandAction toggleDeckAndBottom = new SetCurrentCommandAction() {
			static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				toggleBottomAndDeck();
			}

		};

		toggleDeckAndBottom.putValue(Action.NAME, LanguageResource.getString("TOGGLEDECKBOTTOMBUTTON_STR"));
		toggleDeckAndBottom.putValue(Action.SHORT_DESCRIPTION,
				LanguageResource.getString("TOGGLEDECKBOTTOMBUTTON_STR"));
		toggleDeckAndBottom.putValue(Action.SMALL_ICON,
				new ImageIcon(getClass().getResource("../../icons/BoardCADtoggle24x35.png")));
		mToolBar.add(toggleDeckAndBottom);
		popupMenu.add(toggleDeckAndBottom);

		mToolBar.addSeparator();

		final SetCurrentCommandAction addGuidePoint = new SetCurrentCommandAction(new BrdAddGuidePointCommand());
		addGuidePoint.putValue(Action.NAME, LanguageResource.getString("ADDGUIDEPOINTBUTTON_STR"));
		addGuidePoint.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("ADDGUIDEPOINTBUTTON_STR"));
		addGuidePoint.putValue(Action.SMALL_ICON,
				new ImageIcon(getClass().getResource("../../icons/add-guidepoint.png")));
		mToolBar.add(addGuidePoint);
		popupMenu.add(addGuidePoint);
		popupMenu.add(guidePoints);

		final SetCurrentCommandAction addControlPoint = new SetCurrentOneShotCommandAction(
				new BrdAddControlPointCommand());
		addControlPoint.putValue(Action.NAME, LanguageResource.getString("ADDCONTROLPOINTBUTTON_STR"));
		addControlPoint.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("ADDCONTROLPOINTBUTTON_STR"));
		addControlPoint.putValue(Action.SMALL_ICON,
				new ImageIcon(getClass().getResource("../../icons/add-controlpoint.png")));
		mToolBar.add(addControlPoint);
		popupMenu.add(addControlPoint);

		final SetCurrentCommandAction deleteControlPoint = new SetCurrentCommandAction() {
			static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				BoardEdit edit = getSelectedEdit();
				if (edit == null)
					return;
				ArrayList<BezierKnot> selectedControlPoints = edit.getSelectedControlPoints();

				if (selectedControlPoints.size() == 0) {
					JOptionPane.showMessageDialog(BoardCAD.getInstance().getFrame(),
							LanguageResource.getString("NOCONTROLPOINTSELECTEDMSG_STR"),
							LanguageResource.getString("NOCONTROLPOINTSELECTEDTITLE_STR"), JOptionPane.WARNING_MESSAGE);
					return;
				}

				int selection = JOptionPane.showConfirmDialog(BoardCAD.getInstance().getFrame(),
						LanguageResource.getString("DELETECONTROLPOINTSMSG_STR"),
						LanguageResource.getString("DELETECONTROLPOINTSTITLE_STR"), JOptionPane.WARNING_MESSAGE,
						JOptionPane.YES_NO_OPTION);

				if (selection == JOptionPane.NO_OPTION) {

					return;

				}

				BrdMacroCommand macroCmd = new BrdMacroCommand();
				macroCmd.setSource(edit);
				BezierSpline[] splines = edit.getActiveBezierSplines(edit.getCurrentBrd());

				for (int j = 0; j < splines.length; j++) {
					for (int i = 0; i < selectedControlPoints.size(); i++) {
						BezierKnot ControlPoint = selectedControlPoints.get(i);

						if (ControlPoint == splines[j].getControlPoint(0)
								|| ControlPoint == splines[j].getControlPoint(splines[j].getNrOfControlPoints() - 1)) {
							continue;
						}

						BrdDeleteControlPointCommand deleteControlPointCommand = new BrdDeleteControlPointCommand(edit,
								ControlPoint, splines[j]);

						macroCmd.add(deleteControlPointCommand);

					}
				}

				macroCmd.execute();

				mTabbedPane.repaint();
			}

		};
		deleteControlPoint.putValue(Action.NAME, LanguageResource.getString("DELETECONTROLPOINTSBUTTON_STR"));
		deleteControlPoint.putValue(Action.SHORT_DESCRIPTION,
				LanguageResource.getString("DELETECONTROLPOINTSBUTTON_STR"));
		deleteControlPoint.putValue(Action.SMALL_ICON,
				new ImageIcon(getClass().getResource("../../icons/remove-controlpoint.png")));
		mToolBar.add(deleteControlPoint);
		popupMenu.add(deleteControlPoint);

		popupMenu.addSeparator();
		final SetCurrentCommandAction fitCurveCmd = new SetCurrentCommandAction() {
			static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {

				BoardEdit edit = getSelectedEdit();
				if (edit == null)
					return;

				BrdFitCurveCommand cmd = new BrdFitCurveCommand();
				cmd.execute();
			};

		};
		fitCurveCmd.putValue(Action.NAME, LanguageResource.getString("FITCONTROLPOINTS_STR"));
		fitCurveCmd.putValue(Action.SHORT_DESCRIPTION, LanguageResource.getString("FITCONTROLPOINTS_STR"));
		// addControlPoint.putValue(AbstractAction.SMALL_ICON, new ImageIcon(
		// getClass().getResource("../../icons/add-controlpoint.png")));
		// mToolBar.add(fitCurveCmd);
		popupMenu.add(fitCurveCmd);

		popupMenu.addSeparator();

		mToolBar.addSeparator();
		/*
		 * final JRadioButton millimeterButton = new
		 * JRadioButton(LanguageResource
		 * .getString("MILLIMETERSRADIOBUTTON_STR"));
		 * millimeterButton.addActionListener(new ActionListener() { public void
		 * actionPerformed(final ActionEvent e) {
		 * setCurrentUnit(UnitUtils.MILLIMETERS); } }); final JRadioButton
		 * imperialButton = new
		 * JRadioButton(LanguageResource.getString("FEETINCHESRADIOBUTTON_STR"
		 * )); imperialButton.setSelected(true);
		 * imperialButton.addActionListener(new ActionListener() { public void
		 * actionPerformed(final ActionEvent e) {
		 * setCurrentUnit(UnitUtils.INCHES); } }); final JRadioButton
		 * imperialDecimalButton = new JRadioButton(LanguageResource.getString(
		 * "DECIMALFEETINCHESRADIOBUTTON_STR"));
		 * imperialDecimalButton.addActionListener(new ActionListener() { public
		 * void actionPerformed(final ActionEvent e) {
		 * setCurrentUnit(UnitUtils.INCHES_DECIMAL); } }); final JRadioButton
		 * centimeterButton = new
		 * JRadioButton(LanguageResource.getString("CENTIMETERSRADIOBUTTON_STR"
		 * )); centimeterButton.addActionListener(new ActionListener() { public
		 * void actionPerformed(final ActionEvent e) {
		 * setCurrentUnit(UnitUtils.CENTIMETERS); } });
		 *
		 * final ButtonGroup unitButtonGroup = new ButtonGroup();
		 * unitButtonGroup.add(imperialButton);
		 * unitButtonGroup.add(millimeterButton);
		 * unitButtonGroup.add(centimeterButton);
		 * unitButtonGroup.add(imperialDecimalButton);
		 * mToolBar.add(imperialButton); //mToolBar.addSeparator();
		 * mToolBar.add(millimeterButton); //mToolBar.addSeparator();
		 * mToolBar.add(centimeterButton); //mToolBar.addSeparator();
		 * mToolBar.add(imperialDecimalButton);
		 */
		JLabel unitLabel = new JLabel(LanguageResource.getString("UNIT_STR"));
		unitLabel.setForeground(mSettings.getTextColor());
		mToolBar.add(unitLabel);

		String[] unitsStrList = new String[] { LanguageResource.getString("FEETINCHESRADIOBUTTON_STR"),
				LanguageResource.getString("DECIMALFEETINCHESRADIOBUTTON_STR"),
				LanguageResource.getString("MILLIMETERSRADIOBUTTON_STR"),
				LanguageResource.getString("CENTIMETERSRADIOBUTTON_STR"),
				LanguageResource.getString("METERSRADIOBUTTON_STR") };
		JComboBox unitComboBox = new JComboBox(unitsStrList);
		unitComboBox.setForeground(mSettings.getTextColor());
		unitComboBox.setEditable(false);
		unitComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				unitComboBox.setForeground(mSettings.getTextColor());
				switch (cb.getSelectedIndex()) {
				default:
				case 0:
					setCurrentUnit(UnitUtils.INCHES);
					break;
				case 1:
					setCurrentUnit(UnitUtils.INCHES_DECIMAL);
					break;
				case 2:
					setCurrentUnit(UnitUtils.MILLIMETERS);
					break;
				case 3:
					setCurrentUnit(UnitUtils.CENTIMETERS);
					break;
				case 4:
					setCurrentUnit(UnitUtils.METERS);
					break;
				}
			}
		});
		mToolBar.addSeparator(new Dimension(5, 0));
		mToolBar.add(unitComboBox);
		unitComboBox.setMaximumSize(new Dimension(140, 30));

		mFrame.getContentPane().add(mToolBar, BorderLayout.NORTH);

		final JMenu crossSectionsForPopupMenu = new JMenu(LanguageResource.getString("CROSSECTIONSMENU_STR"));
		crossSectionsForPopupMenu.add(mNextCrossSection);
		crossSectionsForPopupMenu.add(mPreviousCrossSection);
		crossSectionsForPopupMenu.add(addCrossSection);
		crossSectionsForPopupMenu.add(moveCrossSection);
		crossSectionsForPopupMenu.add(deleteCrossSection);
		crossSectionsForPopupMenu.add(copyCrossSection);
		crossSectionsForPopupMenu.add(pasteCrossSection);
		popupMenu.add(crossSectionsForPopupMenu);

		mTabbedPane = new JTabbedPane();

		mQuadView = new QuadView();

		mQuadViewOutlineEdit = new BoardEdit() {
			static final long serialVersionUID = 1L;
			{
				setPreferredSize(new Dimension(300, 200));
				mDrawControl = BezierBoardDrawUtil.MirrorY;
			}

			@Override
			public BezierSpline[] getActiveBezierSplines(final BezierBoard brd) {
				return new BezierSpline[] { brd.getOutline() };
			}

			@Override
			public ArrayList<Point2D.Double> getGuidePoints() {
				return BoardCAD.getInstance().getCurrentBrd().getOutlineGuidePoints();
			}

			@Override
			public void drawPart(final Graphics2D g, final Color color, final Stroke stroke, final BezierBoard brd,
					boolean fill) {
				super.drawPart(g, color, stroke, brd, fill);
				if (isPaintingCenterLine()) {
					drawCenterLine(g, mSettings.getCenterLineColor(), stroke, brd.getLength() / 2.0,
							brd.getCenterWidth() * 1.1);
				}
				if (isPaintingCrossectionsPositions())
					drawOutlineCrossections(this, g, color, stroke, brd);
				if (isPaintingFlowlines())
					drawOutlineFlowlines(this, g, mSettings.getFlowLinesColor(), stroke, brd);
				if (isPaintingTuckUnderLine())
					drawOutlineTuckUnderLine(this, g, mSettings.getTuckUnderLineColor(), stroke, brd);
				if (isPaintingFootMarks() && (brd == getCurrentBrd() || (brd == getGhostBrd() && isGhostMode())
						|| (brd == getOriginalBrd() && isOrgFocus())))
					drawOutlineFootMarks(this, g, new BasicStroke(2.0f / (float) this.mScale), brd);
				drawStringer(g, mSettings.getStringerColor(), stroke, brd);
				if (isPaintingFins()) {
					drawFins(g, mSettings.getFinsColor(), stroke, brd);
				}
			}

			@Override
			public void drawSlidingInfo(final Graphics2D g, final Color color, final Stroke stroke,
					final BezierBoard brd) {
				drawOutlineSlidingInfo(this, g, color, stroke, brd);
			}

			@Override
			public void onBrdChanged() {
				getCurrentBrd().onOutlineChanged();

				super.onBrdChanged();
				mQuadViewCrossSectionEdit.repaint();
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				super.mousePressed(e);

				if (mSelectedControlPoints.size() == 0) {
					final Point pos = e.getPoint();
					final Point2D.Double brdPos = screenCoordinateToBrdCoordinate(pos);
					final int index = getCurrentBrd().getNearestCrossSectionIndex(brdPos.x);
					double tolerance = 5.0;
					if (index != -1 && Math
							.abs(getCurrentBrd().getCrossSections().get(index).getPosition() - brdPos.x) < tolerance) {
						getCurrentBrd().setCurrentCrossSection(index);
					}
					if (getOriginalBrd() != null) {
						final int indexOriginal = getOriginalBrd().getNearestCrossSectionIndex(brdPos.x);
						if (indexOriginal != -1
								&& Math.abs(getOriginalBrd().getCrossSections().get(indexOriginal).getPosition()
										- brdPos.x) < tolerance) {
							getOriginalBrd().setCurrentCrossSection(indexOriginal);
						}
					}
					if (getGhostBrd() != null) {
						final int indexGhost = getGhostBrd().getNearestCrossSectionIndex(brdPos.x);
						if (indexGhost != -1 && Math.abs(getGhostBrd().getCrossSections().get(indexGhost).getPosition()
								- brdPos.x) < tolerance) {
							getGhostBrd().setCurrentCrossSection(indexGhost);
						}
					}
					mQuadViewCrossSectionEdit.repaint();
				}

			}

			@Override
			public void mouseMoved(final MouseEvent e) {

				super.mouseMoved(e);
				mQuadViewCrossSectionEdit.repaint();
			}

		};
		mQuadViewOutlineEdit.add(popupMenu);

		mQuadViewCrossSectionEdit = new BoardEdit() {

			static final long serialVersionUID = 1L;

			{
				mIsCrossSectionEdit = true;
				setPreferredSize(new Dimension(300, 200));
				mDrawControl = BezierBoardDrawUtil.MirrorX | BezierBoardDrawUtil.FlipY;
				mCurvatureScale = 25;
			}

			@Override
			public BezierSpline[] getActiveBezierSplines(final BezierBoard brd) {
				final BezierBoardCrossSection currentCrossSection = brd.getCurrentCrossSection();
				if (currentCrossSection == null)
					return null;

				return new BezierSpline[] { brd.getCurrentCrossSection().getBezierSpline() };
			}

			@Override
			public ArrayList<Point2D.Double> getGuidePoints() {
				final BezierBoardCrossSection currentCrossSection = BoardCAD.getInstance().getCurrentBrd()
						.getCurrentCrossSection();
				if (currentCrossSection == null)
					return null;

				return currentCrossSection.getGuidePoints();
			}

			@Override
			protected boolean isPaintingVolumeDistribution() {
				return false;
			}

			@Override
			public void drawPart(final Graphics2D g, final Color color, final Stroke stroke, final BezierBoard brd,
					boolean fill) {

				if (brd.isEmpty())
					return;

				if (isPaintingNonActiveCrossSections()) {
					final ArrayList<BezierBoardCrossSection> crossSections = brd.getCrossSections();

					final BasicStroke bs = (BasicStroke) stroke;

					final float[] dashPattern = new float[] { 0.8f, 0.2f };
					final BasicStroke stapled = new BasicStroke((float) (bs.getLineWidth() / 2.0), bs.getEndCap(),
							bs.getLineJoin(), bs.getMiterLimit(), dashPattern, 0f);
					final Color noneActiveColor = color.brighter();

					double currentCrossSectionRocker = brd.getRockerAtPos(brd.getCurrentCrossSection().getPosition());

					JavaDraw d = new JavaDraw(g);
					for (int i = 0; i < crossSections.size(); i++) {
						if (crossSections.get(i) != brd.getCurrentCrossSection()) {

							double rockerOffset = 0;
							if (mSettings.isUsingOffsetInterpolation()) {
								rockerOffset = brd.getRockerAtPos(crossSections.get(i).getPosition())
										- currentCrossSectionRocker;
								rockerOffset *= this.mScale;
							}

							BezierBoardDrawUtil.paintBezierSpline(d, mOffsetX, mOffsetY - rockerOffset, mScale, 0.0,
									noneActiveColor, stapled, crossSections.get(i).getBezierSpline(), mDrawControl,
									fill);
						}

					}

				}

				if (isPaintingSlidingCrossSection()) {

					final Color col = (isGhostMode()) ? color : Color.GRAY;

					double pos = mQuadViewRockerEdit.hasMouse() ? mQuadViewRockerEdit.mBrdCoord.x
							: mQuadViewOutlineEdit.mBrdCoord.x;

					double rockerOffset = 0;
					if (mSettings.isUsingOffsetInterpolation()) {
						double currentCrossSectionRocker = brd
								.getRockerAtPos(brd.getCurrentCrossSection().getPosition());
						rockerOffset = brd.getRockerAtPos(pos) - currentCrossSectionRocker;
						rockerOffset *= this.mScale;
					}

					// DEBUG System.out.printf("rockerOffset: %f\n",
					// rockerOffset);

					BezierBoardDrawUtil.paintSlidingCrossSection(new JavaDraw(g), mOffsetX, mOffsetY - rockerOffset,
							mScale, 0.0, col, stroke, (mDrawControl & BezierBoardDrawUtil.FlipX) != 0,
							(mDrawControl & BezierBoardDrawUtil.FlipY) != 0, pos, brd);

					if (isGhostMode()) {
						if (mSettings.isUsingOffsetInterpolation()) {
							double currentCrossSectionRocker = getCurrentBrd()
									.getRockerAtPos(getCurrentBrd().getCurrentCrossSection().getPosition());
							rockerOffset = getCurrentBrd().getRockerAtPos(pos) - currentCrossSectionRocker;
							rockerOffset *= this.mScale;
						}
						BezierBoardDrawUtil.paintSlidingCrossSection(new JavaDraw(g), mOffsetX, mOffsetY - rockerOffset,
								mScale, 0.0, mSettings.getGhostBrdColor(), stroke,
								(mDrawControl & BezierBoardDrawUtil.FlipX) != 0,
								(mDrawControl & BezierBoardDrawUtil.FlipY) != 0, pos, getCurrentBrd());
					}

				}

				super.drawPart(g, color, stroke, brd, fill);

				if (isPaintingCenterLine())
					drawCrossSectionCenterline(this, g, mSettings.getCenterLineColor(), stroke, brd);
				if (isPaintingTuckUnderLine())
					drawCrossSectionTuckUnderLine(this, g, mSettings.getTuckUnderLineColor(), stroke, brd);
				if (isPaintingFlowlines())
					drawCrossSectionFlowlines(this, g, mSettings.getFlowLinesColor(), stroke, brd);
				if (isPaintingApexline())
					drawCrossSectionApexline(this, g, mSettings.getApexLineColor(), stroke, brd);

			}

			@Override
			public void drawBrdCoordinate(Graphics2D g) {
				super.drawBrdCoordinate(g);

				BezierBoard brd = getCurrentBrd();
				if (brd.isEmpty())
					return;

				BezierBoardCrossSection crs = brd.getCurrentCrossSection();
				if (crs == null)
					return;

				g.setColor(Color.BLACK);

				// get metrics from the graphics
				FontMetrics metrics = g.getFontMetrics(mBrdCoordFont);

				// get the height of a line of text in this font and render
				// context
				int hgt = metrics.getHeight();

				String posStr = LanguageResource.getString("CROSSECTIONPOS_STR")
						+ UnitUtils.convertLengthToCurrentUnit(mBoardSpec.isOverCurveSelected()
								? brd.getBottom().getLengthByX(crs.getPosition()) : crs.getPosition(), false)
						+ (mBoardSpec.isOverCurveSelected() ? " O.C" : "");

				g.drawString(posStr, 10, hgt * 3);

				// get the height of a line of text in this font and render
				// context

				String widthStr = LanguageResource.getString("CROSSECTIONWIDTH_STR")
						+ UnitUtils.convertLengthToCurrentUnit(crs.getWidth(), false);

				g.drawString(widthStr, 10, hgt * 4);

				final Dimension dim = getSize();

				String releaseAngleStr = LanguageResource.getString("RELEASEANGLE_STR")
						+ String.format("%1$.1f degrees", crs.getReleaseAngle() / MathUtils.DEG_TO_RAD);

				final int releaseAngleStrLength = metrics.stringWidth(releaseAngleStr);

				g.drawString(releaseAngleStr, dim.width - releaseAngleStrLength - 10, hgt * 1);

				String tuckUnderRadiusStr = LanguageResource.getString("TUCKRADIUS_STR")
						+ UnitUtils.convertLengthToCurrentUnit(crs.getTuckRadius(), false);

				final int tuckUnderRadiusStrLength = metrics.stringWidth(tuckUnderRadiusStr);

				g.drawString(tuckUnderRadiusStr, dim.width - tuckUnderRadiusStrLength - 10, hgt * 2);

			}

			@Override
			public void drawSlidingInfo(final Graphics2D g, final Color color, final Stroke stroke,
					final BezierBoard brd) {

				if (brd.getCrossSections().size() == 0)
					return;

				this.setName("QuadViewCrossSection");

				if (brd.getCurrentCrossSection() == null)
					return;

				BezierBoardCrossSection crs = brd.getCurrentCrossSection();
				final double thickness = crs.getThicknessAtPos(Math.abs(mBrdCoord.x));

				if (thickness <= 0)
					return;

				final double bottom = crs.getBottomAtPos(Math.abs(mBrdCoord.x));
				final double centerThickness = crs.getThicknessAtPos(BezierSpline.ZERO);

				final double mulX = (mDrawControl & BezierBoardDrawUtil.FlipX) != 0 ? -1 : 1;
				final double mulY = (mDrawControl & BezierBoardDrawUtil.FlipY) != 0 ? -1 : 1;

				// get metrics from the graphics
				final FontMetrics metrics = g.getFontMetrics(mSlidingInfoFont);
				// get the height of a line of text in this font and render
				// context
				final int hgt = metrics.getHeight();

				final Dimension dim = getSize();

				String thicknessStr = LanguageResource.getString("CROSSECTIONSLIDINGINFOTHICKNESS_STR");
				mSlidingInfoString = thicknessStr + UnitUtils.convertLengthToCurrentUnit(thickness, false)
						+ String.format("(%02d%%)", (int) ((thickness * 100) / centerThickness));

				g.setColor(Color.BLUE);

				// get the advance of my text in this font and render context
				final int adv = metrics.stringWidth(mSlidingInfoString);

				// calculate the size of a box to hold the text with some
				// padding.
				final Dimension size = new Dimension(adv, hgt + 1);

				// get the advance of my text in this font and render context
				final int advOfThicknessStr = metrics.stringWidth(thicknessStr);

				// calculate the size of a box to hold the text with some
				// padding.
				final Dimension sizeOfThicknessStr = new Dimension(advOfThicknessStr, hgt + 1);

				int textX = mScreenCoord.x - (sizeOfThicknessStr.width);
				if (textX < 10)
					textX = 10;

				if (textX + size.width + 10 > dim.width)
					textX = dim.width - size.width - 10;

				g.setStroke(new BasicStroke((float) (1.0 / mScale)));
				g.drawString(mSlidingInfoString, textX, dim.height - (size.height * 2 + 5));

				mSlidingInfoString = LanguageResource.getString("CROSSECTIONSLIDINGINFOBOTTOM_STR")
						+ UnitUtils.convertLengthToCurrentUnit(bottom, false);

				g.setColor(Color.RED);

				g.drawString(mSlidingInfoString, textX, dim.height - size.height);

				g.setColor(Color.BLACK);

				final double fromCenter = Math.abs(mBrdCoord.x);

				final double fromRail = crs.getWidth() / 2 - Math.abs(mBrdCoord.x);

				mSlidingInfoString = LanguageResource.getString("CROSSECTIONSLIDINGINFOFROMRAIL_STR")
						+ UnitUtils.convertLengthToCurrentUnit(fromRail, false);

				g.drawString(mSlidingInfoString, textX, dim.height - (size.height + 2) * 4);

				mSlidingInfoString = LanguageResource.getString("CROSSECTIONSLIDINGINFOFROMCENTER_STR")
						+ UnitUtils.convertLengthToCurrentUnit(fromCenter, false);

				g.drawString(mSlidingInfoString, textX, dim.height - (size.height + 2) * 3);

				// sets the color of the +ve sliding info (above Y base line)
				g.setColor(Color.BLUE);

				final AffineTransform savedTransform = BezierBoardDrawUtil.setTransform(new JavaDraw(g), mOffsetX,
						mOffsetY, mScale, 0.0);

				mSlidingInfoLine.setLine(mBrdCoord.x * mulX, bottom * mulY, mBrdCoord.x * mulX,
						(bottom + thickness) * mulY);
				g.draw(mSlidingInfoLine);

				// sets the color of the Bottom sliding info (-ve# when concaved
				// +ve# when Vee)
				g.setColor(Color.RED);

				mSlidingInfoLine.setLine(mBrdCoord.x * mulX, 0 * mulY, mBrdCoord.x * mulX, bottom * mulY);
				g.draw(mSlidingInfoLine);

				g.setTransform(savedTransform);

			}

			@Override
			public void fitBrd() {
				final BezierBoard brd = getCurrentBrd();
				final Dimension dim = getSize();

				double width = brd.getCenterWidth();

				mScale = (dim.width - ((BORDER * dim.width / 100) * 2)) / width;

				mOffsetX = dim.width * 1 / 2;
				mOffsetY = dim.height * 1 / 2 + (brd.getThicknessAtPos(brd.getLength() / 2.0f) * mScale);
				// mOffsetY=board_handler.get_edge_offset()/10*mScale+2*dim.height/3;

				mLastWidth = dim.width;
			}

			@Override
			public void onBrdChanged() {
				getCurrentBrd().onCrossSectionChanged();

				mQuadViewOutlineEdit.repaint();
				mQuadViewRockerEdit.repaint();
				super.onBrdChanged();
			}

			@Override
			Point2D.Double getTail() {
				final BezierBoard brd = getCurrentBrd();
				final Point2D.Double tail = (Point2D.Double) getActiveBezierSplines(brd)[0].getControlPoint(0)
						.getEndPoint().clone();

				return tail;
			}

			@Override
			Point2D.Double getNose() {
				final BezierBoard brd = getCurrentBrd();
				final Point2D.Double tail = (Point2D.Double) getActiveBezierSplines(brd)[0].getControlPoint(0)
						.getEndPoint().clone();
				final Point2D.Double nose = (Point2D.Double) getActiveBezierSplines(brd)[0]
						.getControlPoint(getActiveBezierSplines(brd)[0].getNrOfControlPoints() - 1).getEndPoint()
						.clone();
				nose.y = tail.y;
				nose.x = getActiveBezierSplines(brd)[0].getMaxX();
				return nose;
			}

			@Override
			public void repaint() {
				super.repaint();
				if (mCrossSectionOutlineEdit != null)
					mCrossSectionOutlineEdit.repaint();
			}

		};
		mQuadViewCrossSectionEdit.add(popupMenu);

		mQuadViewRockerEdit = new BoardEdit() {
			static final long serialVersionUID = 1L;

			{
				setPreferredSize(new Dimension(300, 200));
				mDrawControl = BezierBoardDrawUtil.FlipY;
				mCurvatureScale = 1000;
			}

			@Override
			public BezierSpline[] getActiveBezierSplines(final BezierBoard brd) {
				switch (mEditDeckorBottom) {
				case DECK:
					return new BezierSpline[] { brd.getDeck() };
				case BOTTOM:
					return new BezierSpline[] { brd.getBottom() };
				case BOTH:
				default:
					return new BezierSpline[] { brd.getDeck(), brd.getBottom() };
				}
			}

			@Override
			public ArrayList<Point2D.Double> getGuidePoints() {
				switch (mEditDeckorBottom) {
				case DECK:
					return BoardCAD.getInstance().getCurrentBrd().getDeckGuidePoints();
				case BOTTOM:
					return BoardCAD.getInstance().getCurrentBrd().getBottomGuidePoints();
				case BOTH:
				default: {
					ArrayList<Point2D.Double> list = new ArrayList<Point2D.Double>();
					list.addAll(BoardCAD.getInstance().getCurrentBrd().getDeckGuidePoints());
					list.addAll(BoardCAD.getInstance().getCurrentBrd().getBottomGuidePoints());
					return list;
				}
				}

			}

			@Override
			public void drawPart(final Graphics2D g, final Color color, final Stroke stroke, final BezierBoard brd,
					boolean fill) {

				if (isPaintingBaseLine()) {
					drawStringer(g, mSettings.getBaseLineColor(),
							new BasicStroke((float) (mSettings.getBaseLineThickness() / mScale)), brd);
				}
				if (isPaintingFlowlines())
					drawProfileFlowlines(this, g, mSettings.getFlowLinesColor(), stroke, brd);
				if (isPaintingApexline())
					drawProfileApexline(this, g, mSettings.getApexLineColor(), stroke, brd);
				if (isPaintingTuckUnderLine())
					drawProfileTuckUnderLine(this, g, mSettings.getTuckUnderLineColor(), stroke, brd);
				if (isPaintingFootMarks() && (brd == getCurrentBrd() || (brd == getGhostBrd() && isGhostMode())
						|| (brd == getOriginalBrd() && isOrgFocus())))
					drawProfileFootMarks(this, g, new BasicStroke(2.0f / (float) this.mScale), brd);
				if (isPaintingBaseLine()) {
					drawStringer(g, mSettings.getBaseLineColor(),
							new BasicStroke((float) (mSettings.getBaseLineThickness() / mScale)), brd);
				}
				if (isPaintingCenterLine()) {
					drawCenterLine(g, mSettings.getCenterLineColor(), stroke, brd.getLength() / 2.0,
							brd.getThickness() * 2.2);
				}

				BezierBoardDrawUtil.paintBezierSplines(new JavaDraw(g), mOffsetX, mOffsetY, mScale, 0.0, color, stroke,
						new BezierSpline[] { brd.getBottom(), brd.getDeck() }, mDrawControl, fill);

				super.drawPart(g, color, stroke, brd, false);
			}

			@Override
			public void drawSlidingInfo(final Graphics2D g, final Color color, final Stroke stroke,
					final BezierBoard brd) {
				drawProfileSlidingInfo(this, g, color, stroke, brd);
			}

			@Override
			public void onBrdChanged() {
				getCurrentBrd().onRockerChanged();

				super.onBrdChanged();
				mQuadViewCrossSectionEdit.repaint();
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				super.mousePressed(e);

				if (mSelectedControlPoints.size() == 0) {
					final Point pos = e.getPoint();
					final Point2D.Double brdPos = screenCoordinateToBrdCoordinate(pos);
					final int index = getCurrentBrd().getNearestCrossSectionIndex(brdPos.x);
					double tolerance = 5.0;
					if (index != -1 && Math
							.abs(getCurrentBrd().getCrossSections().get(index).getPosition() - brdPos.x) < tolerance) {
						getCurrentBrd().setCurrentCrossSection(index);
					}
					if (getOriginalBrd() != null) {
						final int indexOriginal = getOriginalBrd().getNearestCrossSectionIndex(brdPos.x);
						if (indexOriginal != -1 && Math.abs(
								getOriginalBrd().getCrossSections().get(index).getPosition() - brdPos.x) < tolerance) {
							getOriginalBrd().setCurrentCrossSection(indexOriginal);
						}
					}
					if (getGhostBrd() != null) {
						final int indexOriginal = getGhostBrd().getNearestCrossSectionIndex(brdPos.x);
						if (indexOriginal != -1 && Math.abs(
								getGhostBrd().getCrossSections().get(index).getPosition() - brdPos.x) < tolerance) {
							getGhostBrd().setCurrentCrossSection(indexOriginal);
						}
					}
					mQuadViewCrossSectionEdit.repaint();
				}

			}

			@Override
			public void mouseMoved(final MouseEvent e) {

				super.mouseMoved(e);
				mQuadViewCrossSectionEdit.repaint();
			}

		};
		mQuadViewRockerEdit.add(popupMenu);

		mRendered3DView = new ThreeDView();
		mQuad3DView = new ThreeDView();
		mRendered3DView.setBackgroundColor(mSettings.getRenderBackgroundColor());
		mQuad3DView.setBackgroundColor(mSettings.getRenderBackgroundColor());

		mStatusPanel = new StatusPanel();

		mQuadViewOutlineEdit.setParentContainer(mQuadView);
		mQuadViewCrossSectionEdit.setParentContainer(mQuadView);
		mQuadViewRockerEdit.setParentContainer(mQuadView);

		mQuadViewOutlineEdit.setParentContainer(mQuadView);
		mQuadView.add(mQuadViewOutlineEdit);
		mQuadView.add(mQuadViewCrossSectionEdit);
		mQuadView.add(mQuadViewRockerEdit);
		mQuadView.add(mQuad3DView);
		mTabbedPane.add(LanguageResource.getString("QUADVIEW_STR"), mQuadView);

		mOutlineEdit = new BoardEdit() {
			static final long serialVersionUID = 1L;
			{
				mDrawControl = BezierBoardDrawUtil.MirrorY;
			}

			@Override
			public BezierSpline[] getActiveBezierSplines(final BezierBoard brd) {
				return new BezierSpline[] { brd.getOutline() };
			}

			@Override
			public ArrayList<Point2D.Double> getGuidePoints() {
				return BoardCAD.getInstance().getCurrentBrd().getOutlineGuidePoints();
			}

			@Override
			public void drawPart(final Graphics2D g, final Color color, final Stroke stroke, final BezierBoard brd,
					boolean fill) {
				super.drawPart(g, color, stroke, brd, fill);
				if (isPaintingFlowlines())
					drawOutlineFlowlines(this, g, mSettings.getFlowLinesColor(), stroke, brd);
				if (isPaintingTuckUnderLine())
					drawOutlineTuckUnderLine(this, g, mSettings.getTuckUnderLineColor(), stroke, brd);
				if (isPaintingFootMarks() && (brd == getCurrentBrd() || (brd == getGhostBrd() && isGhostMode())
						|| (brd == getOriginalBrd() && isOrgFocus())))
					drawOutlineFootMarks(this, g, new BasicStroke(2.0f / (float) this.mScale), brd);
				if (isPaintingCenterLine()) {
					drawCenterLine(g, mSettings.getCenterLineColor(), stroke, brd.getLength() / 2.0,
							brd.getCenterWidth() * 1.1);
				}
				drawStringer(g, mSettings.getStringerColor(), stroke, brd);
				if (isPaintingFins()) {
					drawFins(g, mSettings.getFinsColor(), stroke, brd);
				}
			}

			@Override
			public void drawSlidingInfo(final Graphics2D g, final Color color, final Stroke stroke,
					final BezierBoard brd) {
				drawOutlineSlidingInfo(this, g, color, stroke, brd);
			}

			@Override
			public void onBrdChanged() {
				getCurrentBrd().onOutlineChanged();

				super.onBrdChanged();
			}

		};
		mOutlineEdit.add(popupMenu);
		mTabbedPane.add(LanguageResource.getString("OUTLINEEDIT_STR"), mOutlineEdit);

		mBottomAndDeckEdit = new BoardEdit() {
			static final long serialVersionUID = 1L;

			{
				setPreferredSize(new Dimension(400, 150));
				mDrawControl = BezierBoardDrawUtil.FlipY;
				mCurvatureScale = 1000;
			}

			@Override
			public BezierSpline[] getActiveBezierSplines(final BezierBoard brd) {
				switch (mEditDeckorBottom) {
				case DECK:
					return new BezierSpline[] { brd.getDeck() };
				case BOTTOM:
					return new BezierSpline[] { brd.getBottom() };
				case BOTH:
				default:
					return new BezierSpline[] { brd.getDeck(), brd.getBottom() };
				}
			}

			@Override
			public ArrayList<Point2D.Double> getGuidePoints() {
				switch (mEditDeckorBottom) {
				case DECK:
					return BoardCAD.getInstance().getCurrentBrd().getDeckGuidePoints();
				case BOTTOM:
					return BoardCAD.getInstance().getCurrentBrd().getBottomGuidePoints();
				case BOTH:
				default: {
					ArrayList<Point2D.Double> list = new ArrayList<Point2D.Double>();
					list.addAll(BoardCAD.getInstance().getCurrentBrd().getDeckGuidePoints());
					list.addAll(BoardCAD.getInstance().getCurrentBrd().getBottomGuidePoints());
					return list;
				}
				}

			}

			@Override
			public void drawPart(final Graphics2D g, final Color color, final Stroke stroke, final BezierBoard brd,
					boolean fill) {
				if (isPaintingFlowlines())
					drawProfileFlowlines(this, g, mSettings.getFlowLinesColor(), stroke, brd);
				if (isPaintingApexline())
					drawProfileApexline(this, g, mSettings.getApexLineColor(), stroke, brd);
				if (isPaintingTuckUnderLine())
					drawProfileTuckUnderLine(this, g, mSettings.getTuckUnderLineColor(), stroke, brd);
				if (isPaintingFootMarks() && (brd == getCurrentBrd() || (brd == getGhostBrd() && isGhostMode())
						|| (brd == getOriginalBrd() && isOrgFocus())))
					drawProfileFootMarks(this, g, new BasicStroke(2.0f / (float) this.mScale), brd);
				if (isPaintingBaseLine()) {
					drawStringer(g, mSettings.getBaseLineColor(),
							new BasicStroke((float) (mSettings.getBaseLineThickness() / mScale)), brd);
				}
				if (isPaintingCenterLine()) {
					drawCenterLine(g, mSettings.getCenterLineColor(), stroke, brd.getLength() / 2.0,
							brd.getThickness() * 2.2);
				}

				BezierBoardDrawUtil.paintBezierSplines(new JavaDraw(g), mOffsetX, mOffsetY, mScale, 0.0, color, stroke,
						new BezierSpline[] { brd.getBottom(), brd.getDeck() }, mDrawControl, fill);

				super.drawPart(g, color, stroke, brd, false);
			}

			@Override
			public void drawSlidingInfo(final Graphics2D g, final Color color, final Stroke stroke,
					final BezierBoard brd) {
				drawProfileSlidingInfo(this, g, color, stroke, brd);
			}

			@Override
			public void onBrdChanged() {
				getCurrentBrd().onRockerChanged();

				super.onBrdChanged();
			}

		};
		mBottomAndDeckEdit.add(popupMenu);

		mTabbedPane.add(LanguageResource.getString("BOTTOMANDDECKEDIT_STR"), mBottomAndDeckEdit);

		/*
		 * mOutlineAndProfileSplitPane = new BrdEditSplitPane(
		 * JSplitPane.VERTICAL_SPLIT, mOutlineEdit2, mBottomAndDeckEdit);
		 * mOutlineAndProfileSplitPane.setOneTouchExpandable(true);
		 * mOutlineAndProfileSplitPane.setResizeWeight(0.7);
		 *
		 * mTabbedPane.add(LanguageResource.getString("OUTLINEPROFILEEDIT_STR"),
		 * mOutlineAndProfileSplitPane);
		 */

		mCrossSectionEdit = new BoardEdit() {
			static final long serialVersionUID = 1L;

			{
				mIsCrossSectionEdit = true;
				setPreferredSize(new Dimension(400, 200));
				mDrawControl = BezierBoardDrawUtil.MirrorX | BezierBoardDrawUtil.FlipY;
				mCurvatureScale = 25;
			}

			@Override
			public BezierSpline[] getActiveBezierSplines(final BezierBoard brd) {
				final BezierBoardCrossSection currentCrossSection = brd.getCurrentCrossSection();
				if (currentCrossSection == null)
					return null;

				return new BezierSpline[] { brd.getCurrentCrossSection().getBezierSpline() };
			}

			@Override
			public ArrayList<Point2D.Double> getGuidePoints() {
				final BezierBoardCrossSection currentCrossSection = BoardCAD.getInstance().getCurrentBrd()
						.getCurrentCrossSection();
				if (currentCrossSection == null)
					return null;

				return currentCrossSection.getGuidePoints();
			}

			@Override
			protected boolean isPaintingVolumeDistribution() {
				return false;
			}

			@Override
			public void drawPart(final Graphics2D g, final Color color, final Stroke stroke, final BezierBoard brd,
					boolean fill) {
				if (brd.isEmpty())
					return;

				if (isPaintingNonActiveCrossSections()) {
					final ArrayList<BezierBoardCrossSection> crossSections = brd.getCrossSections();

					final BasicStroke bs = (BasicStroke) stroke;

					final float[] dashPattern = new float[] { 0.8f, 0.2f };
					final BasicStroke stapled = new BasicStroke((float) (bs.getLineWidth() / 2.0), bs.getEndCap(),
							bs.getLineJoin(), bs.getMiterLimit(), dashPattern, 0f);
					final Color noneActiveColor = color.brighter();

					double currentCrossSectionRocker = brd.getRockerAtPos(brd.getCurrentCrossSection().getPosition());

					for (int i = 0; i < crossSections.size(); i++) {
						if (crossSections.get(i) != brd.getCurrentCrossSection()) {

							double rockerOffset = 0;
							if (mSettings.isUsingOffsetInterpolation()) {
								rockerOffset = brd.getRockerAtPos(crossSections.get(i).getPosition())
										- currentCrossSectionRocker;
								rockerOffset *= this.mScale;
							}

							BezierBoardDrawUtil.paintBezierSpline(new JavaDraw(g), mOffsetX, mOffsetY - rockerOffset,
									mScale, 0.0, noneActiveColor, stapled, crossSections.get(i).getBezierSpline(),
									mDrawControl, fill);
						}

					}
				}

				if (isPaintingSlidingCrossSection()) {

					final Color col = (isGhostMode()) ? color : Color.GRAY;

					double rockerOffset = 0;
					if (mSettings.isUsingOffsetInterpolation()) {
						double currentCrossSectionRocker = brd
								.getRockerAtPos(brd.getCurrentCrossSection().getPosition());
						rockerOffset = brd.getRockerAtPos(mCrossSectionOutlineEdit.mBrdCoord.x)
								- currentCrossSectionRocker;
						rockerOffset *= this.mScale;
					}

					BezierBoardDrawUtil.paintSlidingCrossSection(new JavaDraw(g), mOffsetX, mOffsetY - rockerOffset,
							mScale, 0.0, col, stroke, (mDrawControl & BezierBoardDrawUtil.FlipX) != 0,
							(mDrawControl & BezierBoardDrawUtil.FlipY) != 0, mCrossSectionOutlineEdit.mBrdCoord.x, brd);

					if (isGhostMode()) {
						if (mSettings.isUsingOffsetInterpolation()) {
							double currentCrossSectionRocker = getCurrentBrd()
									.getRockerAtPos(getCurrentBrd().getCurrentCrossSection().getPosition());
							rockerOffset = getCurrentBrd().getRockerAtPos(mCrossSectionOutlineEdit.mBrdCoord.x)
									- currentCrossSectionRocker;
							rockerOffset *= this.mScale;
						}
						BezierBoardDrawUtil.paintSlidingCrossSection(new JavaDraw(g), mOffsetX, mOffsetY - rockerOffset,
								0.0, mScale, mSettings.getGhostBrdColor(), stroke,
								(mDrawControl & BezierBoardDrawUtil.FlipX) != 0,
								(mDrawControl & BezierBoardDrawUtil.FlipY) != 0, mCrossSectionOutlineEdit.mBrdCoord.x,
								getCurrentBrd());
					}

				}
				super.drawPart(g, color, stroke, brd, fill);

				if (isPaintingTuckUnderLine())
					drawCrossSectionTuckUnderLine(this, g, mSettings.getTuckUnderLineColor(), stroke, brd);
				if (isPaintingApexline())
					drawCrossSectionApexline(this, g, mSettings.getApexLineColor(), stroke, brd);
				if (isPaintingFlowlines())
					drawCrossSectionFlowlines(this, g, mSettings.getFlowLinesColor(), stroke, brd);
			}

			@Override
			public void drawBrdCoordinate(Graphics2D g) {
				super.drawBrdCoordinate(g);

				BezierBoard brd = getCurrentBrd();
				if (brd.isEmpty())
					return;

				BezierBoardCrossSection crs = brd.getCurrentCrossSection();
				if (crs == null)
					return;

				g.setColor(Color.BLACK);

				// get metrics from the graphics
				FontMetrics metrics = g.getFontMetrics(mBrdCoordFont);

				// get the height of a line of text in this font and render
				// context
				int hgt = metrics.getHeight();

				String posStr = LanguageResource.getString("CROSSECTIONPOS_STR")
						+ UnitUtils.convertLengthToCurrentUnit(mBoardSpec.isOverCurveSelected()
								? brd.getBottom().getLengthByX(crs.getPosition()) : crs.getPosition(), false)
						+ (mBoardSpec.isOverCurveSelected() ? " O.C" : "");

				g.drawString(posStr, 10, hgt * 3);

				// get the height of a line of text in this font and render
				// context

				String widthStr = LanguageResource.getString("CROSSECTIONWIDTH_STR")
						+ UnitUtils.convertLengthToCurrentUnit(crs.getWidth(), false);

				g.drawString(widthStr, 10, hgt * 4);

				final Dimension dim = getSize();

				String releaseAngleStr = LanguageResource.getString("RELEASEANGLE_STR")
						+ String.format("%1$.1f degrees", crs.getReleaseAngle() / MathUtils.DEG_TO_RAD);

				final int releaseAngleStrLength = metrics.stringWidth(releaseAngleStr);

				g.drawString(releaseAngleStr, dim.width - releaseAngleStrLength - 10, hgt * 1);

				String tuckUnderRadiusStr = LanguageResource.getString("TUCKRADIUS_STR")
						+ UnitUtils.convertLengthToCurrentUnit(crs.getTuckRadius(), false);

				final int tuckUnderRadiusStrLength = metrics.stringWidth(tuckUnderRadiusStr);

				g.drawString(tuckUnderRadiusStr, dim.width - tuckUnderRadiusStrLength - 10, hgt * 2);
			}

			@Override
			public void drawSlidingInfo(final Graphics2D g, final Color color, final Stroke stroke,
					final BezierBoard brd) {
				if (brd.getCrossSections().size() == 0)
					return;

				if (brd.getCurrentCrossSection() == null)
					return;

				BezierBoardCrossSection crs = brd.getCurrentCrossSection();
				final double thickness = crs.getThicknessAtPos(Math.abs(mBrdCoord.x));

				if (thickness <= 0)
					return;

				final double bottom = crs.getBottomAtPos(Math.abs(mBrdCoord.x));
				final double centerThickness = crs.getThicknessAtPos(BezierSpline.ZERO);

				final double mulX = (mDrawControl & BezierBoardDrawUtil.FlipX) != 0 ? -1 : 1;
				final double mulY = (mDrawControl & BezierBoardDrawUtil.FlipY) != 0 ? -1 : 1;

				// get metrics from the graphics
				final FontMetrics metrics = g.getFontMetrics(mSlidingInfoFont);
				// get the height of a line of text in this font and render
				// context
				final int hgt = metrics.getHeight();

				final Dimension dim = getSize();

				String thicknessStr = LanguageResource.getString("CROSSECTIONSLIDINGINFOTHICKNESS_STR");
				mSlidingInfoString = thicknessStr + UnitUtils.convertLengthToCurrentUnit(thickness, false)
						+ String.format("(%02d%%)", (int) ((thickness * 100) / centerThickness));

				g.setColor(Color.BLUE);

				// get the advance of my text in this font and render context
				final int adv = metrics.stringWidth(mSlidingInfoString);

				// calculate the size of a box to hold the text with some
				// padding.
				final Dimension size = new Dimension(adv, hgt + 1);

				// get the advance of my text in this font and render context
				final int advOfThicknessStr = metrics.stringWidth(thicknessStr);

				// calculate the size of a box to hold the text with some
				// padding.
				final Dimension sizeOfThicknessStr = new Dimension(advOfThicknessStr, hgt + 1);

				int textX = mScreenCoord.x - (sizeOfThicknessStr.width);
				if (textX < 10)
					textX = 10;

				if (textX + size.width + 10 > dim.width)
					textX = dim.width - size.width - 10;

				g.setStroke(new BasicStroke((float) (1.0 / mScale)));
				g.drawString(mSlidingInfoString, textX, dim.height - (size.height * 2 + 5));

				mSlidingInfoString = LanguageResource.getString("CROSSECTIONSLIDINGINFOBOTTOM_STR")
						+ UnitUtils.convertLengthToCurrentUnit(bottom, false);
				g.setColor(Color.RED);

				g.drawString(mSlidingInfoString, textX, dim.height - size.height);

				g.setColor(Color.BLACK);

				final double fromCenter = Math.abs(mBrdCoord.x);

				final double fromRail = crs.getWidth() / 2.0 - Math.abs(mBrdCoord.x);

				mSlidingInfoString = LanguageResource.getString("CROSSECTIONSLIDINGINFOFROMRAIL_STR")
						+ UnitUtils.convertLengthToCurrentUnit(fromRail, false);

				g.drawString(mSlidingInfoString, textX, dim.height - (size.height + 2) * 4);

				mSlidingInfoString = LanguageResource.getString("CROSSECTIONSLIDINGINFOFROMCENTER_STR")
						+ UnitUtils.convertLengthToCurrentUnit(fromCenter, false);

				g.drawString(mSlidingInfoString, textX, dim.height - (size.height + 2) * 3);

				// sets the color of the +ve sliding info (above Y base line)
				g.setColor(Color.BLUE);

				final AffineTransform savedTransform = BezierBoardDrawUtil.setTransform(new JavaDraw(g), mOffsetX,
						mOffsetY, mScale, 0.0);

				mSlidingInfoLine.setLine(mBrdCoord.x * mulX, bottom * mulY, mBrdCoord.x * mulX,
						(bottom + thickness) * mulY);
				g.draw(mSlidingInfoLine);

				// sets the color of the Bottom sliding info (-ve# when concaved
				// +ve# when Vee)
				g.setColor(Color.RED);

				mSlidingInfoLine.setLine(mBrdCoord.x * mulX, 0 * mulY, mBrdCoord.x * mulX, bottom * mulY);
				g.draw(mSlidingInfoLine);

				g.setTransform(savedTransform);

			}

			@Override
			public void fitBrd() {
				final BezierBoard brd = getCurrentBrd();
				final Dimension dim = getSize();

				double width = brd.getCenterWidth();

				mScale = (dim.width - ((BORDER * dim.width / 100) * 2)) / width;

				mOffsetX = dim.width * 1 / 2;
				mOffsetY = dim.height * 1 / 2 + (brd.getThicknessAtPos(brd.getLength() / 2.0f) * mScale);
				// mOffsetY=board_handler.get_edge_offset()/10*mScale+2*dim.height/3;

				mLastWidth = dim.width;
			}

			@Override
			public void onBrdChanged() {
				getCurrentBrd().onCrossSectionChanged();

				super.onBrdChanged();
			}

			@Override
			Point2D.Double getTail() {
				final BezierBoard brd = getCurrentBrd();
				final Point2D.Double tail = (Point2D.Double) getActiveBezierSplines(brd)[0].getControlPoint(0)
						.getEndPoint().clone();

				return tail;
			}

			@Override
			Point2D.Double getNose() {
				final BezierBoard brd = getCurrentBrd();
				final Point2D.Double tail = (Point2D.Double) getActiveBezierSplines(brd)[0].getControlPoint(0)
						.getEndPoint().clone();
				final Point2D.Double nose = (Point2D.Double) getActiveBezierSplines(brd)[0]
						.getControlPoint(getActiveBezierSplines(brd)[0].getNrOfControlPoints() - 1).getEndPoint()
						.clone();
				nose.y = tail.y;
				nose.x = getActiveBezierSplines(brd)[0].getMaxX();
				return nose;
			}

			@Override
			public void repaint() {
				super.repaint();
				if (mCrossSectionOutlineEdit != null)
					mCrossSectionOutlineEdit.repaint();
			}

		};
		mCrossSectionEdit.add(popupMenu);

		mCrossSectionOutlineEdit = new BoardEdit() {
			static final long serialVersionUID = 1L;

			static final double fixedHeightBorder = 0;
			{
				setPreferredSize(new Dimension(400, 100));
				mDrawControl = BezierBoardDrawUtil.MirrorY;
			};

			@Override
			public void paintComponent(final Graphics g) {
				fitBrd();
				super.paintComponent(g);
			}

			@Override
			public void fitBrd() {
				super.fitBrd();

				final Dimension dim = getSize();

				final BezierBoard brd = getCurrentBrd();
				final double width = brd.getCenterWidth() + brd.getMaxRocker() * 2;
				if (dim.height - (fixedHeightBorder * 2) < width * mScale) {
					mScale = (dim.height - (fixedHeightBorder * 2)) / width;

					if ((mDrawControl & BezierBoardDrawUtil.FlipX) == 0) {
						mOffsetX = (dim.width - (brd.getLength() * mScale)) / 2;
					} else {
						mOffsetX = (dim.width - (brd.getLength() * mScale)) / 2 + brd.getLength() * mScale;
					}
				}

				mOffsetY -= brd.getMaxRocker() / 2 * mScale;

			}

			@Override
			public void drawPart(final Graphics2D g, final Color color, final Stroke stroke, final BezierBoard brd,
					boolean fill) {

				Color brdColor = mSettings.getBrdColor();
				Color current = BoardCAD.getInstance().isGhostMode() ? mSettings.getGhostBrdColor() : color;
				current = BoardCAD.getInstance().isOrgFocus() ? mSettings.getOriginalBrdColor() : current;

				BezierBoardDrawUtil.paintBezierSpline(new JavaDraw(g), mOffsetX, mOffsetY, mScale, 0.0, current, stroke,
						brd.getOutline(), mDrawControl, fill);

				if (isPaintingFlowlines())
					BezierBoardDrawUtil.paintOutlineFlowLines(new JavaDraw(g), mOffsetX, mOffsetY, mScale, 0.0,
							mSettings.getFlowLinesColor(), stroke, brd, (mDrawControl & BezierBoardDrawUtil.FlipX) != 0,
							true);

				if (isPaintingTuckUnderLine())
					BezierBoardDrawUtil.paintOutlineTuckUnderLine(new JavaDraw(g), mOffsetX, mOffsetY, mScale, 0.0,
							mSettings.getTuckUnderLineColor(), stroke, brd,
							(mDrawControl & BezierBoardDrawUtil.FlipX) != 0, true);

				BezierBoardDrawUtil.paintBezierSplines(new JavaDraw(g), mOffsetX,
						mOffsetY + ((brd.getCenterWidth() / 2.0 + brd.getMaxRocker()) * mScale), mScale, 0.0, current,
						stroke, new BezierSpline[] { brd.getDeck(), brd.getBottom() },
						(mDrawControl & BezierBoardDrawUtil.FlipX) | BezierBoardDrawUtil.FlipY, fill);

				if (isPaintingFlowlines())
					BezierBoardDrawUtil.paintProfileFlowLines(new JavaDraw(g), mOffsetX,
							mOffsetY + (((brd.getCenterWidth() / 2 + brd.getMaxRocker())) * mScale), mScale, 0.0,
							mSettings.getFlowLinesColor(), stroke, brd, (mDrawControl & BezierBoardDrawUtil.FlipX) != 0,
							true);

				if (isPaintingApexline())
					BezierBoardDrawUtil.paintProfileApexline(new JavaDraw(g), mOffsetX,
							mOffsetY + (((brd.getCenterWidth() / 2 + brd.getMaxRocker())) * mScale), mScale, 0.0,
							mSettings.getApexLineColor(), stroke, brd, (mDrawControl & BezierBoardDrawUtil.FlipX) != 0,
							true);

				if (isPaintingTuckUnderLine())
					BezierBoardDrawUtil.paintProfileTuckUnderLine(new JavaDraw(g), mOffsetX,
							mOffsetY + (((brd.getCenterWidth() / 2 + brd.getMaxRocker())) * mScale), mScale, 0.0,
							mSettings.getTuckUnderLineColor(), stroke, brd,
							(mDrawControl & BezierBoardDrawUtil.FlipX) != 0, true);

				BezierBoard ghost = BoardCAD.getInstance().getGhostBrd();
				if (BoardCAD.getInstance().isGhostMode() && ghost != null && !ghost.isEmpty()) {
					BezierBoardDrawUtil.paintBezierSpline(new JavaDraw(g), mOffsetX, mOffsetY, mScale, 0.0, brdColor,
							stroke, ghost.getOutline(), mDrawControl, fill);

					BezierBoardDrawUtil.paintBezierSplines(new JavaDraw(g), mOffsetX,
							mOffsetY + (((brd.getCenterWidth() / 2 + brd.getMaxRocker())) * mScale), mScale, 0.0,
							brdColor, stroke, new BezierSpline[] { ghost.getDeck(), ghost.getBottom() },
							(mDrawControl & BezierBoardDrawUtil.FlipX) | BezierBoardDrawUtil.FlipY, fill);

				}

				BezierBoard org = BoardCAD.getInstance().getOriginalBrd();
				if (BoardCAD.getInstance().isOrgFocus() && org != null && !org.isEmpty()) {
					BezierBoardDrawUtil.paintBezierSpline(new JavaDraw(g), mOffsetX, mOffsetY, mScale, 0.0, brdColor,
							stroke, org.getOutline(), mDrawControl, fill);

					BezierBoardDrawUtil.paintBezierSplines(new JavaDraw(g), mOffsetX,
							mOffsetY + (((brd.getCenterWidth() / 2 + brd.getMaxRocker())) * mScale), mScale, 0.0,
							brdColor, stroke, new BezierSpline[] { org.getDeck(), org.getBottom() },
							(mDrawControl & BezierBoardDrawUtil.FlipX) | BezierBoardDrawUtil.FlipY, fill);

				}

				final AffineTransform savedTransform = g.getTransform();

				g.setColor(color);

				g.setStroke(stroke);

				final AffineTransform at = new AffineTransform();

				at.setToTranslation(mOffsetX, mOffsetY);

				g.transform(at);

				at.setToScale(mScale, mScale);

				g.transform(at);

				final double mulX = ((mDrawControl & BezierBoardDrawUtil.FlipX) != 0) ? -1 : 1;
				final double mulY = ((mDrawControl & BezierBoardDrawUtil.FlipY) != 0) ? -1 : 1;

				final ArrayList<BezierBoardCrossSection> crossSections = brd.getCrossSections();
				final Line2D line = new Line2D.Double();
				for (int i = 1; i < crossSections.size() - 1; i++) {
					final double pos = crossSections.get(i).getPosition();
					double width = brd.getWidthAtPos(pos);

					if (crossSections.get(i) == brd.getCurrentCrossSection()) {
						g.setColor(Color.RED);
					} else {
						g.setColor(color);
					}
					line.setLine(pos * mulX, (-width / 2) * mulY, pos * mulX, (width / 2) * mulY);
					g.draw(line);
				}

				if (BoardCAD.getInstance().isGhostMode() && ghost != null && !ghost.isEmpty()) {
					final ArrayList<BezierBoardCrossSection> ghostCrossSections = ghost.getCrossSections();
					for (int i = 1; i < ghostCrossSections.size() - 1; i++) {
						final double pos = ghostCrossSections.get(i).getPosition();
						double width = ghost.getWidthAtPos(pos);

						if (ghostCrossSections.get(i) == ghost.getCurrentCrossSection()) {
							g.setColor(Color.RED);
						} else {
							g.setColor(color);
						}
						line.setLine(pos * mulX, (-width / 2) * mulY, pos * mulX, (width / 2) * mulY);
						g.draw(line);
					}

				}

				if (BoardCAD.getInstance().isOrgFocus() && org != null && !org.isEmpty()) {
					final ArrayList<BezierBoardCrossSection> orgCrossSections = org.getCrossSections();
					for (int i = 1; i < orgCrossSections.size() - 1; i++) {
						final double pos = orgCrossSections.get(i).getPosition();
						double width = org.getWidthAtPos(pos);

						if (orgCrossSections.get(i) == org.getCurrentCrossSection()) {
							g.setColor(Color.RED);
						} else {
							g.setColor(color);
						}
						line.setLine(pos * mulX, (-width / 2) * mulY, pos * mulX, (width / 2) * mulY);
						g.draw(line);
					}

				}

				g.setTransform(savedTransform);

			}

			@Override
			public void drawSlidingInfo(final Graphics2D g, final Color color, final Stroke stroke,
					final BezierBoard brd) {
				drawOutlineSlidingInfo(this, g, color, stroke, brd);
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				final Point pos = e.getPoint();
				final Point2D.Double brdPos = screenCoordinateToBrdCoordinate(pos);
				final int index = getCurrentBrd().getNearestCrossSectionIndex(brdPos.x);
				if (index != -1) {
					getCurrentBrd().setCurrentCrossSection(index);
				}
				if (getOriginalBrd() != null) {
					final int indexOriginal = getOriginalBrd().getNearestCrossSectionIndex(brdPos.x);
					if (indexOriginal != -1) {
						getOriginalBrd().setCurrentCrossSection(indexOriginal);
					}
				}
				if (getGhostBrd() != null) {
					final int indexOriginal = getGhostBrd().getNearestCrossSectionIndex(brdPos.x);
					if (indexOriginal != -1) {
						getGhostBrd().setCurrentCrossSection(indexOriginal);
					}
				}
				mCrossSectionSplitPane.repaint();

			}

			@Override
			public void mouseMoved(final MouseEvent e) {

				super.mouseMoved(e);
				mCrossSectionEdit.repaint();
			}

		};

		mCrossSectionSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mCrossSectionEdit, mCrossSectionOutlineEdit);
		mCrossSectionSplitPane.setOneTouchExpandable(true);
		mCrossSectionSplitPane.setResizeWeight(0.7);

		mTabbedPane.add(LanguageResource.getString("CROSSECTIONEDIT_STR"), mCrossSectionSplitPane);

		mRenderedPanel = new JPanel();
		mRenderedPanel.setLayout(new BorderLayout());

		mRenderedPanel.add(mRendered3DView, BorderLayout.CENTER);
		mRenderedPanel.add(mStatusPanel, BorderLayout.SOUTH);

		mTabbedPane.addTab(LanguageResource.getString("3DRENDEREDVIEW_STR"), mRenderedPanel);

		// DEBUG!
		mTabbedPane.add("PrintBrd", mPrintBrd); // Only for debugging
		mTabbedPane.add("PrintSpecSheet", mPrintSpecSheet); // Only for
															// debugging
		mTabbedPane.add("PrintChamberedWood", mPrintChamberedWoodTemplate); // Only
																			// for
																			// debugging
		mTabbedPane.add("PrintSandwich", mPrintSandwichTemplates); // Only for
																	// debugging
		mTabbedPane.add("PrintHWS", mPrintHollowWoodTemplates); // Only for
																// debugging

		mTabbedPane.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				mGuidePointsDialog.update();

				if (mTabbedPane.getSelectedComponent() == mRenderedPanel
						|| mTabbedPane.getSelectedComponent() == mQuadView) {

					boolean selected = mShowBezier3DModelMenuItem.getModel().isSelected();
					if (selected) {
						updateBezier3DModel();
					}

					mRendered3DView.redraw();
					mQuad3DView.redraw();
				}

			}
		});

		final JMenu pluginMenu = new JMenu(LanguageResource.getString("PLUGINSMENU_STR"));

		final AbstractPluginHandler pluginLoader = new AbstractPluginHandler() {
			static final long serialVersionUID = 1L;

			@Override
			public void onNewPluginMenu(JMenu menu) {
				pluginMenu.add(menu);
			}

			@Override
			public void onNewPluginComponent(JComponent component) {
				mTabbedPane.add(component);
			}
		};
		pluginLoader.loadPlugins("plugins");
		if (pluginMenu.getItemCount() > 0) {
			menuBar.add(pluginMenu);
		}

		mTabbedPane2 = new JTabbedPane(SwingConstants.BOTTOM);

		panel = new JPanel();

		panel.setLayout(new BorderLayout());

		panel.add(mStatusPanel, BorderLayout.NORTH);

		mControlPointInfo = new ControlPointInfo();
		panel.add(mControlPointInfo, BorderLayout.EAST);

		mBoardSpec = new BoardSpec();
		panel.add(mBoardSpec, BorderLayout.WEST);

		mTabbedPane2 = new JTabbedPane(SwingConstants.BOTTOM);
		mTabbedPane2.addTab("Board specification", panel);

		mSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mTabbedPane, mTabbedPane2);
		mSplitPane.setResizeWeight(1.0);
		mSplitPane.setOneTouchExpandable(true);
		mFrame.getContentPane().add(mSplitPane, BorderLayout.CENTER);
		Dimension mindim = new Dimension(0, 0);
		mTabbedPane.setMinimumSize(mindim);
		mTabbedPane2.setMinimumSize(mindim);
		mTabbedPane.setPreferredSize(new Dimension(600, 230));
		mTabbedPane2.setPreferredSize(new Dimension(600, 230));

		// load jython script
		String scriptname = "boardcad_init.py";
		File file = new File(scriptname);
		if (file.exists()) {
			ScriptLoader.loadScript(scriptname);
		}

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width * 9 / 10;
		int height = screenSize.height * 9 / 10;
		mFrame.setLocation((screenSize.width - width) / 2, (screenSize.height - height) / 2);

		mFrame.setSize(width, height);

		mFrame.setVisible(true);

		edit.actionPerformed(null);

		mWeightCalculatorDialog = new WeightCalculatorDialog();
		mWeightCalculatorDialog.setModal(false);
		mWeightCalculatorDialog.setAlwaysOnTop(true);
		mWeightCalculatorDialog.setVisible(false);

		mGuidePointsDialog = new BoardGuidePointsDialog();
		mGuidePointsDialog.setModal(false);
		mGuidePointsDialog.setAlwaysOnTop(true);
		mGuidePointsDialog.setVisible(false);

		// Set current unit after all and everything is initialized
		unitComboBox.setSelectedIndex(1);

		// DEBUG
		/*
		 * mIsPaintingGridMenuItem.setSelected(false);
		 * mIsPaintingOriginalBrdMenuItem.setSelected(false);
		 * mIsPaintingGhostBrdMenuItem.setSelected(false);
		 * mIsPaintingControlPointsMenuItem.setSelected(true);
		 * mIsPaintingNonActiveCrossSectionsMenuItem.setSelected(false);
		 * mIsPaintingGuidePointsMenuItem.setSelected(false);
		 * mIsPaintingCurvatureMenuItem.setSelected(false);
		 * mIsPaintingSlidingInfoMenuItem.setSelected(true);
		 * mIsPaintingSlidingCrossSectionMenuItem.setSelected(false);
		 * mIsPaintingFinsMenuItem.setSelected(false);
		 * mIsPaintingBackgroundImageMenuItem.setSelected(true);
		 * mIsAntialiasingMenuItem.setSelected(false);
		 * BrdReader.loadFile(getCurrentBrd(),
		 * DefaultBrds.getInstance().getBoardArray("Funboard"), "funboard");
		 * mOriginalBrd.set(getCurrentBrd()); fitAll(); onBrdChanged();
		 *
		 * getSelectedEdit().loadBackgroundImage("F:\\Gfx\\Misc\\Surfboards\\
		 * horan 6'8 keelboard outline.jpg");
		 */

		getPreferences();

		mGUIBlocked = false;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {

		final String cmdStr = e.getActionCommand();
		if (cmdStr == LanguageResource.getString("PRINTOUTLINE_STR")) {
			CategorizedSettings settings = new CategorizedSettings();
			String categoryName = LanguageResource.getString("PRINTOUTLINEPARAMETERSCATEGORY_STR");
			Settings printOutlineSettings = settings.addCategory(categoryName);
			printOutlineSettings.addBoolean("PrintGrid", true, LanguageResource.getString("PRINTGRID_STR"));
			printOutlineSettings.addBoolean("OverCurve", false, LanguageResource.getString("PRINTOVERCURVE_STR"));
			SettingDialog settingsDialog = new SettingDialog(settings);
			settingsDialog.setTitle(LanguageResource.getString("PRINTOUTLINEPARAMETERSTITLE_STR"));
			settingsDialog.setModal(true);
			settingsDialog.setVisible(true);
			settingsDialog.dispose();
			if (settingsDialog.wasCancelled()) {
				return;
			}

			mPrintBrd.printOutline(printOutlineSettings.getBoolean("PrintGrid"),
					printOutlineSettings.getBoolean("OverCurve"));

		} else if (cmdStr == LanguageResource.getString("PRINTSPINTEMPLATE_STR")) {
			CategorizedSettings settings = new CategorizedSettings();
			String categoryName = LanguageResource.getString("PRINTSPINTEMPLATEPARAMETERSCATEGORY_STR");
			Settings printOutlineSettings = settings.addCategory(categoryName);
			printOutlineSettings.addBoolean("PrintGrid", true, LanguageResource.getString("PRINTGRID_STR"));
			printOutlineSettings.addBoolean("OverCurve", false, LanguageResource.getString("OVERCURVE_STR"));
			SettingDialog settingsDialog = new SettingDialog(settings);
			settingsDialog.setTitle(LanguageResource.getString("PRINTSPINTEMPLATEPARAMETERSTITLE_STR"));
			settingsDialog.setModal(true);
			settingsDialog.setVisible(true);
			settingsDialog.dispose();
			if (settingsDialog.wasCancelled()) {
				return;
			}

			mPrintBrd.printSpinTemplate(printOutlineSettings.getBoolean("PrintGrid"),
					printOutlineSettings.getBoolean("OverCurve"));
		} else if (cmdStr == LanguageResource.getString("PRINTPROFILE_STR")) {
			mPrintBrd.printProfile();
		} else if (cmdStr == LanguageResource.getString("PRINTCROSSECTION_STR")) {
			mPrintBrd.printSlices();
			// } else if (cmdStr ==
			// LanguageResource.getString("PRINTSPECSHEET_STR")) {
			// mPrintBrd.printSpecSheet();
			// } else if (cmdStr == LanguageResource.getString("VIEW3D_STR")) {
			// design_panel.view_3d();
			// } else if (cmdStr == LanguageResource.getString("EDITNURBS_STR"))
			// {
			// design_panel.view_all();
			// design_panel.fit_all();
		}

	}

	@Override
	public void itemStateChanged(final ItemEvent e) {

		mFrame.repaint();

	}

	@Override
	public boolean dispatchKeyEvent(final KeyEvent e) {
		if (mControlPointInfo != null && mControlPointInfo.isEditing())
			return false;

		if (mGuidePointsDialog != null && mGuidePointsDialog.isVisible() && mGuidePointsDialog.isFocused())
			return false;

		if (mWeightCalculatorDialog != null && mWeightCalculatorDialog.isVisible()
				&& mWeightCalculatorDialog.isFocused())
			return false;

		BoardEdit edit = getSelectedEdit();
		if (edit == null)
			return false;

		if (this.getFrame().getFocusOwner() == null)
			return false;

		// System.out.printf("dispatchKeyEvent() event %s\n",e.toString());

		switch (e.getKeyCode()) {
		case KeyEvent.VK_ADD:
			if (e.getID() == KeyEvent.KEY_PRESSED) {
				mNextCrossSection.actionPerformed(null);
			}
			break;

		case KeyEvent.VK_SUBTRACT:
			if (e.getID() == KeyEvent.KEY_PRESSED) {
				mPreviousCrossSection.actionPerformed(null);
			}
			break;

		case KeyEvent.VK_G:
			if (e.getID() == KeyEvent.KEY_PRESSED) {
				if (e.isControlDown())
					break;

				if (mGhostMode == false) {
					mGhostMode = true;
					if (mPreviousCommand == null) {
						mPreviousCommand = getCurrentCommand();
						setCurrentCommand(new GhostCommand());
					}
					if (edit != null)
						edit.repaint();
					mBoardSpec.updateInfoInstantly();
				}
			} else if (e.getID() == KeyEvent.KEY_RELEASED) {
				if (mGhostMode == true) {
					mGhostMode = false;
					if (mPreviousCommand != null) {
						setCurrentCommand(mPreviousCommand);
						mPreviousCommand = null;
					}
					if (edit != null)
						edit.repaint();
					mBoardSpec.updateInfoInstantly();
				}
			}
			return true;

		case KeyEvent.VK_O:

			if (e.getID() == KeyEvent.KEY_PRESSED) {
				if (e.isControlDown())
					break;

				if (mOrgFocus != true) {
					mOrgFocus = true;
					if (edit != null)
						edit.repaint();
					mBoardSpec.updateInfoInstantly();
				}
			} else if (e.getID() == KeyEvent.KEY_RELEASED) {
				if (mOrgFocus != false) {
					mOrgFocus = false;
					if (edit != null)
						edit.repaint();
					mBoardSpec.updateInfoInstantly();
				}
			}
			return true;
		case KeyEvent.VK_ESCAPE:
			setCurrentCommand(new BrdEditCommand());
			if (edit != null)
				edit.repaint();
			break;
		}

		if (isGhostMode()) {
			if (e.getID() == KeyEvent.KEY_PRESSED) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_UP:
					if (edit != null)
						edit.mGhostOffsetY -= (e.isAltDown() ? .1f : 1f) / edit.getScale();
					mFrame.repaint();
					return true;
				case KeyEvent.VK_DOWN:
					if (edit != null)
						edit.mGhostOffsetY += (e.isAltDown() ? .1f : 1f) / edit.getScale();
					mFrame.repaint();
					return true;
				case KeyEvent.VK_LEFT:
					if (edit != null)
						edit.mGhostOffsetX -= (e.isAltDown() ? .1f : 1f) / edit.getScale();
					mFrame.repaint();
					return true;
				case KeyEvent.VK_RIGHT:
					if (edit != null)
						edit.mGhostOffsetX += (e.isAltDown() ? .1f : 1f) / edit.getScale();
					mFrame.repaint();
					return true;
				case KeyEvent.VK_Q:
					if (edit != null)
						edit.mGhostRot -= (Math.PI / 180.0f) * (e.isAltDown() ? .1f : 1f) / edit.getScale();
					mFrame.repaint();
					return true;
				case KeyEvent.VK_W:
					if (edit != null)
						edit.mGhostRot += (Math.PI / 180.0f) * (e.isAltDown() ? .1f : 1f) / edit.getScale();
					mFrame.repaint();
					return true;
				default:
					return false;
				}
			}

		}
		if (mOrgFocus) {
			if (e.getID() == KeyEvent.KEY_PRESSED) {

				switch (e.getKeyCode()) {
				case KeyEvent.VK_UP:
					if (edit != null)
						edit.mOriginalOffsetY -= (e.isAltDown() ? .1f : 1f) / edit.getScale();
					mFrame.repaint();
					return true;
				case KeyEvent.VK_DOWN:
					if (edit != null)
						edit.mOriginalOffsetY += (e.isAltDown() ? .1f : 1f) / edit.getScale();
					mFrame.repaint();
					return true;
				case KeyEvent.VK_LEFT:
					if (edit != null)
						edit.mOriginalOffsetX -= (e.isAltDown() ? .1f : 1f) / edit.getScale();
					mFrame.repaint();
					return true;
				case KeyEvent.VK_RIGHT:
					if (edit != null)
						edit.mOriginalOffsetX += (e.isAltDown() ? .1f : 1f) / edit.getScale();
					mFrame.repaint();
					return true;
				default:
					return false;
				}
			}
		}

		if (edit == null)
			return false;

		final BrdInputCommand cmd = (BrdInputCommand) edit.getCurrentCommand();
		if (cmd != null) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_T:
				if (e.getID() == KeyEvent.KEY_PRESSED) {
					if (mPreviousCommand == null) {
						mPreviousCommand = getCurrentCommand();
						setCurrentCommand(new SetImageTailCommand());
					}
				} else if (e.getID() == KeyEvent.KEY_RELEASED) {
					if (mPreviousCommand != null) {
						setCurrentCommand(mPreviousCommand);
						mPreviousCommand = null;
					}
				}
				return true;

			case KeyEvent.VK_N:
				if (e.getID() == KeyEvent.KEY_PRESSED) {
					if (mPreviousCommand == null) {
						mPreviousCommand = getCurrentCommand();
						setCurrentCommand(new SetImageNoseCommand());
					}
				} else if (e.getID() == KeyEvent.KEY_RELEASED) {
					if (mPreviousCommand != null) {
						setCurrentCommand(mPreviousCommand);
						mPreviousCommand = null;
					}
				}
				return true;

			}

			return cmd.onKeyEvent(edit, e);
		} else {
			return false;
		}
	}

	public void toggleBottomAndDeck() {
		switch (mEditDeckorBottom) {
		case DECK:
			mEditDeckorBottom = DeckOrBottom.BOTTOM;
			break;
		case BOTTOM:
			mEditDeckorBottom = DeckOrBottom.BOTH;
			break;
		case BOTH:
			mEditDeckorBottom = DeckOrBottom.DECK;
			break;
		}
		mBottomAndDeckEdit.repaint();

		mBottomAndDeckEdit.mSelectedControlPoints.clear();

		redraw();
	}

	public void drawOutlineFootMarks(final BoardEdit source, final Graphics2D g, final Stroke stroke,
			final BezierBoard brd) {

		if (brd.isEmpty())
			return;

		g.setStroke(stroke);

		Point centerPoint = source.brdCoordinateToScreenCoordinateTo(new Point2D.Double(brd.getLength() / 2.0, 0.0));
		Point widthPoint = source.brdCoordinateToScreenCoordinateTo(new Point2D.Double(0, brd.getMaxWidth() / 2));

		// get metrics from the graphics
		final FontMetrics metrics = g.getFontMetrics(source.mSlidingInfoFont);
		// get the height of a line of text in this font and render context
		final int hgt = metrics.getHeight();

		for (int i = 0; i < 7; i++) {
			double pos = 0.0;
			String label;

			if (i < 3) {
				pos = (i == 0) ? UnitUtils.INCH : i * UnitUtils.INCH * UnitUtils.INCHES_PR_FOOT;
				label = UnitUtils.convertLengthToCurrentUnit(pos, false);
			} else if (i == 3) {
				pos = brd.getMaxWidthPos();
				label = "W.P:" + UnitUtils.convertLengthToCurrentUnit(mBoardSpec.isOverCurveSelected()
						? brd.getBottom().getPointByCurveLength(pos).x
								- brd.getBottom().getPointByCurveLength(brd.getLength() / 2.0).x
						: pos - brd.getLength() / 2.0, false);
			} else {
				pos = (mBoardSpec.isOverCurveSelected() ? brd.getBottom().getLength() : brd.getLength())
						- ((i == 6) ? UnitUtils.INCH : (6 - i) * UnitUtils.INCH * UnitUtils.INCHES_PR_FOOT);
				label = UnitUtils.convertLengthToCurrentUnit(
						-((i == 6) ? UnitUtils.INCH : (6 - i) * UnitUtils.INCH * UnitUtils.INCHES_PR_FOOT), false);
			}

			if (mBoardSpec.isOverCurveSelected()) {
				pos = brd.getBottom().getPointByCurveLength(pos).x;

				label = label.concat(" O.C");
			}

			double width = brd.getWidthAt(pos);

			String widthStr = UnitUtils.convertLengthToCurrentUnit(width, false);

			g.setColor(Color.BLUE);

			// get the advance of my text in this font and render context
			final int labelWidth = metrics.stringWidth(label);

			// get the advance of my text in this font and render context
			final int widthOfWidthString = metrics.stringWidth(widthStr);

			Point outlinePoint = source.brdCoordinateToScreenCoordinateTo(new Point2D.Double(pos, width / 2.0));
			Point upperOutlinePoint = source.brdCoordinateToScreenCoordinateTo(new Point2D.Double(pos, -width / 2.0));

			g.setColor(Color.BLACK);

			g.drawString(label, outlinePoint.x - (labelWidth / 2), centerPoint.y);

			g.setColor(Color.BLUE);

			g.drawLine(outlinePoint.x, upperOutlinePoint.y, outlinePoint.x, outlinePoint.y);

			g.drawString(widthStr, outlinePoint.x - (widthOfWidthString / 2), widthPoint.y + hgt);

			g.setColor(Color.DARK_GRAY);

			g.drawLine(outlinePoint.x, outlinePoint.y, outlinePoint.x, widthPoint.y);
		}

	}

	public void drawProfileFootMarks(final BoardEdit source, final Graphics2D g, final Stroke stroke,
			final BezierBoard brd) {

		g.setStroke(stroke);

		Point centerPoint = source.brdCoordinateToScreenCoordinateTo(new Point2D.Double(brd.getLength() / 2.0, 0.0));
		Point maxThicknessPoint = source
				.brdCoordinateToScreenCoordinateTo(new Point2D.Double(0, brd.getMaxThickness()));
		Point maxRockerPoint = source.brdCoordinateToScreenCoordinateTo(new Point2D.Double(0, brd.getMaxRocker()));
		Point bottomPoint = source.brdCoordinateToScreenCoordinateTo(new Point2D.Double(0, 0));

		// get metrics from the graphics
		final FontMetrics metrics = g.getFontMetrics(source.mSlidingInfoFont);
		// get the height of a line of text in this font and render context
		final int hgt = metrics.getHeight();

		for (int i = 0; i < 7; i++) {
			double pos = 0.0;
			String label;

			if (i < 3) {
				pos = (i == 0) ? 0.001 : i * UnitUtils.INCH * UnitUtils.INCHES_PR_FOOT;
				label = (i == 0) ? "" : UnitUtils.convertLengthToCurrentUnit(pos, false);
			} else if (i == 3) {
				pos = brd.getLength() / 2.0;
				label = "Center: " + UnitUtils.convertLengthToCurrentUnit(
						mBoardSpec.isOverCurveSelected() ? brd.getBottom().getPointByCurveLength(pos).x : pos, false);
			} else {
				pos = (mBoardSpec.isOverCurveSelected() ? brd.getBottom().getLength() : brd.getLength())
						- ((i == 6) ? 0.005 : (6 - i) * UnitUtils.INCH * UnitUtils.INCHES_PR_FOOT);
				label = (i == 6) ? ""
						: UnitUtils.convertLengthToCurrentUnit(-(6 - i) * UnitUtils.INCH * UnitUtils.INCHES_PR_FOOT,
								false);
			}

			if (mBoardSpec.isOverCurveSelected()) {
				pos = brd.getBottom().getPointByCurveLength(pos).x;

				if (label != "")
					label = label.concat(" O.C");
			}

			double thickness = brd.getThicknessAtPos(pos);
			double rocker = brd.getRockerAtPos(pos);

			String thicknessStr = UnitUtils.convertLengthToCurrentUnit(thickness, false);
			String rockerStr = UnitUtils.convertLengthToCurrentUnit(rocker, false);

			g.setColor(Color.BLUE);

			// get the advance of my text in this font and render context
			final int labelWidth = metrics.stringWidth(label);

			// get the advance of my text in this font and render context
			final int widthOfThicknessString = metrics.stringWidth(thicknessStr);
			final int widthOfRockerString = metrics.stringWidth(rockerStr);

			Point deckPoint = source.brdCoordinateToScreenCoordinateTo(new Point2D.Double(pos, rocker + thickness));
			Point rockerPoint = source.brdCoordinateToScreenCoordinateTo(new Point2D.Double(pos, rocker));

			g.setColor(Color.BLACK);

			g.drawString(label, deckPoint.x - (labelWidth / 2), (maxThicknessPoint.y + maxRockerPoint.y) / 2);

			g.setColor(Color.RED);

			g.drawString(thicknessStr, deckPoint.x - (widthOfThicknessString / 2), bottomPoint.y + hgt);

			g.drawLine(deckPoint.x, deckPoint.y, rockerPoint.x, rockerPoint.y);

			g.setColor(Color.BLUE);

			g.drawString(rockerStr, deckPoint.x - (widthOfRockerString / 2), bottomPoint.y + hgt * 2);

			g.drawLine(rockerPoint.x, rockerPoint.y, rockerPoint.x, bottomPoint.y);
		}

	}

	public void drawOutlineSlidingInfo(final BoardEdit source, final Graphics2D g, final Color color,
			final Stroke stroke, final BezierBoard brd) {

		final double width = brd.getWidthAtPos(source.mBrdCoord.x);
		if (width <= 0.0)
			return;

		final double mulX = (source.mDrawControl & BezierBoardDrawUtil.FlipX) != 0 ? -1 : 1;
		final double mulY = (source.mDrawControl & BezierBoardDrawUtil.FlipY) != 0 ? -1 : 1;

		String widthStr = LanguageResource.getString("OUTLINESLIDINGINFOWIDTH_STR");
		source.mSlidingInfoString = widthStr + UnitUtils.convertLengthToCurrentUnit(width, false);

		g.setColor(Color.BLUE);

		// get metrics from the graphics
		final FontMetrics metrics = g.getFontMetrics(source.mSlidingInfoFont);
		// get the height of a line of text in this font and render context
		final int hgt = metrics.getHeight();

		// get the advance of my text in this font and render context
		final int adv = metrics.stringWidth(source.mSlidingInfoString);

		// calculate the size of a box to hold the text with some padding.
		final Dimension size = new Dimension(adv, hgt + 1);
		final Dimension dim = source.getSize();

		// get the advance of my text in this font and render context
		final int advOfWidthStr = metrics.stringWidth(widthStr);

		// calculate the size of a box to hold the text with some padding.
		final Dimension sizeOfWidthStr = new Dimension(advOfWidthStr, hgt + 1);

		int textX = source.mScreenCoord.x - (sizeOfWidthStr.width);
		if (textX < 10)
			textX = 10;

		if (textX + size.width + 10 > dim.width)
			textX = dim.width - size.width - 10;

		if (BoardCAD.getInstance().isPaintingOverCurveMeasurements()) {
			source.mSlidingInfoString = LanguageResource.getString("OUTLINESLIDINGINFOOVERCURVE_STR");

			g.setColor(Color.BLACK);

			g.drawString(source.mSlidingInfoString, textX, dim.height - (size.height + 2) * 4);

			final double fromNose = brd.getFromNoseOverBottomCurveAtPos(source.mBrdCoord.x);

			final double fromTail = brd.getFromTailOverBottomCurveAtPos(source.mBrdCoord.x);

			source.mSlidingInfoString = LanguageResource.getString("OUTLINESLIDINGINFOFROMTAIL_STR")
					+ UnitUtils.convertLengthToCurrentUnit(fromTail, false);

			g.drawString(source.mSlidingInfoString, textX, dim.height - (size.height + 2) * 3);

			source.mSlidingInfoString = LanguageResource.getString("OUTLINESLIDINGINFOFROMNOSE_STR") + (" ")
					+ UnitUtils.convertLengthToCurrentUnit(fromNose, false);

			g.drawString(source.mSlidingInfoString, textX, dim.height - (size.height + 2) * 2);

		}

		if (BoardCAD.getInstance().isPaintingMomentOfInertia()) {
			final double momentOfInertia = brd.getMomentOfInertia(source.mBrdCoord.x, source.mBrdCoord.y);

			source.mSlidingInfoString = LanguageResource.getString("SLIDINGINFOMOMENTOFINERTIA_STR")
					+ UnitUtils.convertMomentOfInertiaToCurrentUnit(momentOfInertia);

			g.drawString(source.mSlidingInfoString, textX, dim.height
					- (size.height + 2) * (BoardCAD.getInstance().isPaintingOverCurveMeasurements() ? 5 : 2));
		}

		source.mSlidingInfoString = LanguageResource.getString("OUTLINESLIDINGINFOWIDTH_STR")
				+ UnitUtils.convertLengthToCurrentUnit(width, false);

		g.setColor(Color.BLUE);

		g.drawString(source.mSlidingInfoString, textX, dim.height - (size.height + 2) * 1);

		final AffineTransform savedTransform = BezierBoardDrawUtil.setTransform(new JavaDraw(g), source.mOffsetX,
				source.mOffsetY, source.mScale, 0.0);

		source.mSlidingInfoLine.setLine(source.mBrdCoord.x * mulX, -(width / 2) * mulY, source.mBrdCoord.x * mulX,
				(width / 2) * mulY);

		g.draw(source.mSlidingInfoLine);

		g.setTransform(savedTransform);
	}

	public void drawOutlineCrossections(final BoardEdit source, final Graphics2D g, final Color color,
			final Stroke stroke, final BezierBoard brd) {

		final AffineTransform savedTransform = BezierBoardDrawUtil.setTransform(new JavaDraw(g), source.mOffsetX,
				source.mOffsetY, source.mScale, 0.0, (source.mDrawControl & BezierBoardDrawUtil.FlipX) != 0,
				(source.mDrawControl & BezierBoardDrawUtil.FlipY) != 0);

		Line2D.Double crossSectionLine = new Line2D.Double(); // @jve:decl-index=0:

		BezierBoardCrossSection currentCrossSection = brd.getCurrentCrossSection();

		final float[] dashPattern = new float[] { 5.0f, 1.0f };
		final BasicStroke bs = (BasicStroke) stroke;
		final BasicStroke stapled = new BasicStroke((float) (bs.getLineWidth() / 2.0), bs.getEndCap(), bs.getLineJoin(),
				bs.getMiterLimit(), dashPattern, 0f);
		final Color noneActiveColor = color.brighter();

		for (int i = 0; i < brd.getCrossSections().size(); i++) {
			BezierBoardCrossSection tmp = brd.getCrossSections().get(i);

			if (tmp == currentCrossSection) {
				g.setColor(color);
				g.setStroke(stroke);
			} else {
				g.setColor(noneActiveColor);
				g.setStroke(stapled);
			}

			double pos = tmp.getPosition();

			final double width = brd.getWidthAtPos(pos);
			if (width <= 0.0)
				continue;

			crossSectionLine.setLine(pos, -(width / 2), pos, (width / 2));

			g.draw(crossSectionLine);
		}

		g.setTransform(savedTransform);
	}

	public void drawProfileSlidingInfo(final BoardEdit source, final Graphics2D g, final Color color,
			final Stroke stroke, final BezierBoard brd) {

		final double thickness = brd.getThicknessAtPos(source.mBrdCoord.x);
		if (thickness <= 0.0)
			return;

		final double rocker = brd.getRockerAtPos(source.mBrdCoord.x);
		final double curvature = source.getActiveBezierSplines(brd)[0].getCurvatureAt(source.mBrdCoord.x);
		final double radius = 1 / curvature;

		final double mulX = (source.mDrawControl & BezierBoardDrawUtil.FlipX) != 0 ? -1 : 1;
		final double mulY = (source.mDrawControl & BezierBoardDrawUtil.FlipY) != 0 ? -1 : 1;

		// get metrics from the graphics
		final FontMetrics metrics = g.getFontMetrics(source.mSlidingInfoFont);
		// get the height of a line of text in this font and render context
		final int hgt = metrics.getHeight();

		final Dimension dim = source.getSize();

		String thicknessStr = LanguageResource.getString("PROFILESLIDINGINFOTHICKNESS_STR");
		source.mSlidingInfoString = thicknessStr + UnitUtils.convertLengthToCurrentUnit(thickness, false);

		// get the advance of my text in this font and render context
		final int adv = metrics.stringWidth(source.mSlidingInfoString);

		// calculate the size of a box to hold the text with some padding.
		final Dimension size = new Dimension(adv, hgt + 1);

		// get the advance of my text in this font and render context
		final int advOfThicknessStr = metrics.stringWidth(thicknessStr);

		// calculate the size of a box to hold the text with some padding.
		final Dimension sizeOfThicknessStr = new Dimension(advOfThicknessStr, hgt + 1);

		int textX = source.mScreenCoord.x - (sizeOfThicknessStr.width);
		if (textX < 10)
			textX = 10;

		if (textX + size.width + 10 > dim.width)
			textX = dim.width - size.width - 10;

		g.setColor(Color.BLUE);

		g.drawString(source.mSlidingInfoString, textX, dim.height - (size.height + 2) * 3);

		g.setColor(Color.RED);

		source.mSlidingInfoString = LanguageResource.getString("PROFILESLIDINGINFOROCKER_STR")
				+ UnitUtils.convertLengthToCurrentUnit(rocker, false);

		g.drawString(source.mSlidingInfoString, textX, dim.height - (size.height + 2) * 2);

		source.mSlidingInfoString = LanguageResource.getString("PROFILESLIDINGINFORADIUS_STR")
				+ UnitUtils.convertLengthToCurrentUnit(radius, true);

		g.setColor(new Color(102, 102, 102));

		g.drawString(source.mSlidingInfoString, textX, dim.height - (size.height + 2) * 1);

		if (BoardCAD.getInstance().isPaintingOverCurveMeasurements()) {
			source.mSlidingInfoString = LanguageResource.getString("PROFILESLIDINGINFOOVERCURVE_STR");

			g.setColor(Color.BLACK);

			g.drawString(source.mSlidingInfoString, textX, dim.height - (size.height + 2) * 6);

			final double fromNose = brd.getFromNoseOverBottomCurveAtPos(source.mBrdCoord.x);

			final double fromTail = brd.getFromTailOverBottomCurveAtPos(source.mBrdCoord.x);

			source.mSlidingInfoString = LanguageResource.getString("PROFILESLIDINGINFOFROMTAIL_STR")
					+ UnitUtils.convertLengthToCurrentUnit(fromTail, false);

			g.drawString(source.mSlidingInfoString, textX, dim.height - (size.height + 2) * 5);

			source.mSlidingInfoString = LanguageResource.getString("PROFILESLIDINGINFOFROMNOSE_STR")
					+ UnitUtils.convertLengthToCurrentUnit(fromNose, false);

			g.drawString(source.mSlidingInfoString, textX, dim.height - (size.height + 2) * 4);

		}

		final AffineTransform savedTransform = BezierBoardDrawUtil.setTransform(new JavaDraw(g), source.mOffsetX,
				source.mOffsetY, source.mScale, 0.0);

		// sets the color of the thickness sliding info bar (inside board)
		g.setColor(Color.BLUE);

		source.mSlidingInfoLine.setLine(source.mBrdCoord.x * mulX, rocker * mulY, source.mBrdCoord.x * mulX,
				(rocker + thickness) * mulY);

		g.draw(source.mSlidingInfoLine);

		// sets the color of the rocker sliding info bar (outside board)
		g.setColor(Color.RED);

		source.mSlidingInfoLine.setLine(source.mBrdCoord.x * mulX, 0 * mulY, source.mBrdCoord.x * mulX, rocker * mulY);

		g.draw(source.mSlidingInfoLine);

		g.setTransform(savedTransform);

	}

	public void drawProfileCrossections(final BoardEdit source, final Graphics2D g, final Color color,
			final Stroke stroke, final BezierBoard brd) {

		final double mulX = (source.mDrawControl & BezierBoardDrawUtil.FlipX) != 0 ? -1 : 1;
		final double mulY = (source.mDrawControl & BezierBoardDrawUtil.FlipY) != 0 ? -1 : 1;

		final AffineTransform savedTransform = BezierBoardDrawUtil.setTransform(new JavaDraw(g), source.mOffsetX,
				source.mOffsetY, source.mScale, 0.0);

		Line2D.Double crossSectionLine = new Line2D.Double(); // @jve:decl-index=0:

		BezierBoardCrossSection currentCrossSection = brd.getCurrentCrossSection();

		final float[] dashPattern = new float[] { 5.0f, 1.0f };
		final BasicStroke bs = (BasicStroke) stroke;
		final BasicStroke stapled = new BasicStroke((float) (bs.getLineWidth() / 2.0), bs.getEndCap(), bs.getLineJoin(),
				bs.getMiterLimit(), dashPattern, 0f);
		final Color noneActiveColor = color.brighter();

		for (int i = 0; i < brd.getCrossSections().size(); i++) {
			BezierBoardCrossSection tmp = brd.getCrossSections().get(i);

			if (tmp == currentCrossSection) {
				g.setColor(color);
				g.setStroke(stroke);
			} else {
				g.setColor(noneActiveColor);
				g.setStroke(stapled);
			}

			double pos = tmp.getPosition();
			final double deck = brd.getDeckAtPos(pos);
			final double rocker = brd.getRockerAtPos(pos);

			if (deck <= 0.0)
				continue;

			crossSectionLine.setLine(pos * mulX, deck * mulY, pos * mulX, rocker * mulY);

			g.draw(crossSectionLine);
		}

		g.setTransform(savedTransform);
	}

	public void drawOutlineFlowlines(final BoardEdit source, final Graphics2D g, final Color color, final Stroke stroke,
			final BezierBoard brd) {
		BezierBoardDrawUtil.paintOutlineFlowLines(new JavaDraw(g), source.mOffsetX, source.mOffsetY, source.mScale, 0.0,
				color, stroke, brd, (source.mDrawControl & BezierBoardDrawUtil.FlipX) != 0, true);
	}

	public void drawOutlineTuckUnderLine(final BoardEdit source, final Graphics2D g, final Color color,
			final Stroke stroke, final BezierBoard brd) {
		BezierBoardDrawUtil.paintOutlineTuckUnderLine(new JavaDraw(g), source.mOffsetX, source.mOffsetY, source.mScale,
				0.0, color, stroke, brd, (source.mDrawControl & BezierBoardDrawUtil.FlipX) != 0, true);
	}

	public void drawProfileFlowlines(final BoardEdit source, final Graphics2D g, final Color color, final Stroke stroke,
			final BezierBoard brd) {
		BezierBoardDrawUtil.paintProfileFlowLines(new JavaDraw(g), source.mOffsetX, source.mOffsetY, source.mScale, 0.0,
				color, stroke, brd, (source.mDrawControl & BezierBoardDrawUtil.FlipX) != 0, true);
	}

	public void drawProfileApexline(final BoardEdit source, final Graphics2D g, final Color color, final Stroke stroke,
			final BezierBoard brd) {
		BezierBoardDrawUtil.paintProfileApexline(new JavaDraw(g), source.mOffsetX, source.mOffsetY, source.mScale, 0.0,
				color, stroke, brd, (source.mDrawControl & BezierBoardDrawUtil.FlipX) != 0, true);
	}

	public void drawProfileTuckUnderLine(final BoardEdit source, final Graphics2D g, final Color color,
			final Stroke stroke, final BezierBoard brd) {
		BezierBoardDrawUtil.paintProfileTuckUnderLine(new JavaDraw(g), source.mOffsetX, source.mOffsetY, source.mScale,
				0.0, color, stroke, brd, (source.mDrawControl & BezierBoardDrawUtil.FlipX) != 0, true);
	}

	public void drawCrossSectionCenterline(final BoardEdit source, final Graphics2D g, final Color color,
			final Stroke stroke, final BezierBoard brd) {
		boolean isCompensatedForRocker = mSettings.isUsingOffsetInterpolation();
		BezierBoardDrawUtil.paintCrossSectionCenterline(new JavaDraw(g), source.mOffsetX,
				source.mOffsetY + (isCompensatedForRocker
						? brd.getRockerAtPos(brd.getCurrentCrossSection().getPosition()) * source.mScale : 0),
				source.mScale, 0.0, color, stroke, brd, true, !isCompensatedForRocker);
	}

	public void drawCrossSectionFlowlines(final BoardEdit source, final Graphics2D g, final Color color,
			final Stroke stroke, final BezierBoard brd) {
		boolean isCompensatedForRocker = mSettings.isUsingOffsetInterpolation();
		BezierBoardDrawUtil.paintCrossSectionFlowLines(new JavaDraw(g), source.mOffsetX,
				source.mOffsetY + (isCompensatedForRocker
						? brd.getRockerAtPos(brd.getCurrentCrossSection().getPosition()) * source.mScale : 0),
				source.mScale, 0.0, color, stroke, brd, true, !isCompensatedForRocker);
	}

	public void drawCrossSectionApexline(final BoardEdit source, final Graphics2D g, final Color color,
			final Stroke stroke, final BezierBoard brd) {
		boolean isCompensatedForRocker = mSettings.isUsingOffsetInterpolation();
		BezierBoardDrawUtil.paintCrossSectionApexline(new JavaDraw(g), source.mOffsetX,
				source.mOffsetY + (isCompensatedForRocker
						? brd.getRockerAtPos(brd.getCurrentCrossSection().getPosition()) * source.mScale : 0),
				source.mScale, 0.0, color, stroke, brd, true, !isCompensatedForRocker);
	}

	public void drawCrossSectionTuckUnderLine(final BoardEdit source, final Graphics2D g, final Color color,
			final Stroke stroke, final BezierBoard brd) {
		boolean isCompensatedForRocker = mSettings.isUsingOffsetInterpolation();
		BezierBoardDrawUtil.paintCrossSectionTuckUnderLine(new JavaDraw(g), source.mOffsetX,
				source.mOffsetY + (isCompensatedForRocker
						? brd.getRockerAtPos(brd.getCurrentCrossSection().getPosition()) * source.mScale : 0),
				source.mScale, 0.0, color, stroke, brd, true, !isCompensatedForRocker);
	}

	static public Frame findParentFrame(Container container) {
		while (container != null) {
			if (container instanceof Frame) {
				return (Frame) container;
			}

			container = container.getParent();
		}
		return null;
	}
}

class SetCurrentCommandAction extends AbstractAction {
	static final long serialVersionUID = 1L;
	BrdCommand mCommand;

	SetCurrentCommandAction() {

	}

	SetCurrentCommandAction(final BrdCommand command) {
		mCommand = command;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		if (BoardCAD.getInstance().getCurrentCommand() != null) {
			BoardCAD.getInstance().getCurrentCommand().onCurrentChanged();
		}

		BoardCAD.getInstance().setCurrentCommand(mCommand);

		mCommand.onSetCurrent();

		BoardCAD.getInstance().getFrame().repaint();
	}

}

class SetCurrentOneShotCommandAction extends SetCurrentCommandAction {
	static final long serialVersionUID = 1L;

	SetCurrentOneShotCommandAction(final BrdCommand command) {
		super(command);
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		mCommand.setPreviousCommand(BoardCAD.getInstance().getCurrentCommand());

		super.actionPerformed(event);
	}

}
