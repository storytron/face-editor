import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class FaceEditor extends JFrame implements ActionListener, ItemListener {
	static final long serialVersionUID = 1l;
	public static final 	Color transparent = new Color(0,0,0,0);	// transparency color
	public static final int WindowWidth = 2000;
	public static final int WindowHeight = 1050;
	static final Font numeralFont = new Font("Arial", Font.PLAIN, 10);
	static final Font labelFont = new Font("Arial", Font.PLAIN, 12);
	static final int nullExpression = 2;

	// These are the buttons holding the different Expressions.
	// At present there are only three: null, test1, and test2. 
	// The null Expression should not be altered.
	// The Expressions are stored in file 'res/expressions.xml'
	public ArrayList<JRadioButton> emotionButtons = new ArrayList<JRadioButton>();
	public ArrayList<JRadioButton> excursionButtons = new ArrayList<JRadioButton>();
	public ArrayList<JRadioButton> percentAButtons = new ArrayList<JRadioButton>();
	public ArrayList<JRadioButton> percentBButtons = new ArrayList<JRadioButton>();
	public ArrayList<JRadioButton> lineThicknessButtons = new ArrayList<JRadioButton>();
	
	// Points for handling the dragging of control points.
	// A control point is a tiny square that can be dragged to change the shape of a Feature.
	Point startPoint, draggedPoint, previousPoint;
	
	// Java AWT containers to hold the groups of buttons
	Container buttonContainer;
	Container emotionContainer;
	Container excursionContainer;
	Container numberContainer;
	
	// These buttons copy one side of the expression to the other side.
	Button mirrorRightButton, mirrorLeftButton;	
	
	// indeces for the currently selected Actor, Expression, Feature, and Point.
	static int iExpression, iFeature, iPoint, iExcursion, iEmotion, iMagEmotion, iMagExcursion, iLineThickness;
	static float emoFraction, excFraction;
	
	// whether or not the numeric labels should be displayed next to their control points.
	static boolean isLabelled = false;
	
	// whether the currently selected control point is on the right or left side of the face.
	static boolean isRightSide; 
	
	// controls whether or not numeric labels should be shown.
	static Checkbox labelCheckBox;
	
	// used for copying and pasting entire Expressions.
	static Button copyButton, pasteButton;
	
	// the contents of the "Expression Clipboard", as it were
	static int copiedExpression = 0;
	
	// The highest level data structure, containing both ActorFaces and Expressions
	static FaceDisplay faceDisplay;
	
	static Timer attackTimer, sustainTimer, decayTimer;
	static int animationStep;
	static Timer intervalTimer;
	static Timer irisTimer;
	static Image offscreen;
	static Graphics2D backBuffer;
	static boolean isAnimation; 
	Expression displayExpression; 
	
// ************************************************************
	public FaceEditor() { // conventional initialization stuff
		super("Face Editor Version 4.0");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(200, 50, WindowWidth, WindowHeight);
		setSize(WindowWidth, WindowHeight);
		setLayout(null);
		setBackground(Color.white);
		setVisible(true);
		iExpression = 0;
		iExcursion = 0;
		iEmotion = 0;
		iMagEmotion = 10;
		iMagExcursion = 10;
		emoFraction = 1.0f;
		excFraction = 1.0f;
		isAnimation = false;
		faceDisplay = new FaceDisplay();
		
		iFeature = 0;
		iPoint = 0;
		startPoint = null;
		draggedPoint = null;
		previousPoint = null;
		animationStep = 0;
		offscreen = createImage(WindowWidth, WindowHeight);
		backBuffer = (Graphics2D)offscreen.getGraphics();
		
		iLineThickness = 3;
			
		addMouseMotionListener(new MouseMotionListener() {	
			@Override
			public void mouseDragged(MouseEvent e) {
				if ((startPoint!=null) & (iExpression>1)) {
					int deltaX = 0;
					int deltaY = 0;
					if (draggedPoint !=null) {
						previousPoint = draggedPoint;
						deltaX = (int)e.getX() - (int)previousPoint.getX();
						deltaY = (int)e.getY() - (int)previousPoint.getY();
					}
					draggedPoint = e.getPoint();
					if (iFeature == Expression.Nose) {
						for (int i = 0; (i<Expression.FeatureCount); ++i) {
							Feature ff = faceDisplay.getExpression((iExpression)).getFeature(i);
							for (int j = 0; (j<ff.getSize()); ++j) {
								ff.setRightX(j, ff.getRightX(j)+deltaX);
								ff.setRightY(j, ff.getRightY(j)+deltaY);
								ff.setLeftX(j, ff.getLeftX(j)-deltaX);
								ff.setLeftY(j, ff.getLeftY(j)+deltaY);								
							}
						}
					}
					else {
						Feature f = faceDisplay.getExpression((iExpression)).getFeature(iFeature);
						if (isRightSide) {
							f.setRightX(iPoint, f.getRightX(iPoint)+deltaX);
							f.setRightY(iPoint, f.getRightY(iPoint)+deltaY);
						}
						else {
							f.setLeftX(iPoint, f.getLeftX(iPoint)-deltaX);
							f.setLeftY(iPoint, f.getLeftY(iPoint)+deltaY);
						}
					}
					if (iEmotion > 0)
						displayExpression.buildExpression(faceDisplay.getExpression(nullExpression), 
								faceDisplay.getExpression(iEmotion), emoFraction, 
								faceDisplay.getExpression(iExcursion), excFraction, 0.0f);
					if (iExcursion > 1) 
						displayExpression.buildExpression(faceDisplay.getExpression(nullExpression), 
								faceDisplay.getExpression(iEmotion), emoFraction, 
								faceDisplay.getExpression(iExcursion), excFraction, 1.0f);
					repaint();
				}
			}
			@Override
			public void mouseMoved(MouseEvent e) {
			}			
		} );
		
		addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) { }
			public void mouseEntered(MouseEvent arg0) {					
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));					
			}
			public void mouseExited(MouseEvent arg0) {					
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));										
			}
			public void mousePressed(MouseEvent e) {
				if (iExpression>nullExpression) {
					for (int i = 0; (i<Expression.FeatureCount); ++i) {
						Feature ff = faceDisplay.getExpression((iExpression)).getFeature(i);
						boolean gotcha = false;
						int j = 0;
						while ((j<ff.getSize()) & !gotcha) {
							Rectangle testRect = new Rectangle(ff.getRightBigX(j), ff.getRightBigY(j),8,8);
							if (testRect.contains(e.getPoint())) {
								gotcha = true;
								startPoint = e.getPoint();
								iFeature = i;
								iPoint =j;
								isRightSide = true;
								resetLineThicknessButton(ff);
//								repaint();
							}
							testRect = new Rectangle(ff.getLeftBigX(j), ff.getLeftBigY(j),8,8);
							if (testRect.contains(e.getPoint())) {
								gotcha = true;
								startPoint = e.getPoint();
								iFeature = i;
								iPoint =j;
								isRightSide = false;
								resetLineThicknessButton(ff);
//								repaint();
							}
							++j;
						}
					}
				}
			}

			public void mouseReleased(MouseEvent e) {
				startPoint = null;
				draggedPoint = null;
//				repaint(e.getX()-100,e.getY()-100,200,200);
			}
		} );
				
		buttonContainer = new Container();
		buttonContainer.setBounds(0, WindowHeight-190, 700, 150);
		buttonContainer.setVisible(true);

		ButtonGroup lineThicknessGroup = new ButtonGroup();			
		int y3 = 25;
		for (int i=0; (i<6); ++i) {
			JRadioButton testButton = new JRadioButton(String.valueOf((int)(100*Feature.getLineThickness(i)))+"_%");
			testButton.addActionListener(this);
			testButton.setVisible(true);
			testButton.setBackground(Color.white);
			testButton.setBounds(10, y3, 100, 18);
			lineThicknessButtons.add(testButton);
			lineThicknessGroup.add(testButton);
			buttonContainer.add(testButton);
			y3 += 18;
		}
		lineThicknessButtons.get(3).setSelected(true);
		

		
		labelCheckBox = new Checkbox("Labels");
		labelCheckBox.addItemListener(this);
		labelCheckBox.setBounds(100, 0, 100, 30);
		labelCheckBox.setVisible(true);
		labelCheckBox.setBackground(Color.white);
		labelCheckBox.setState(false);
		buttonContainer.add(labelCheckBox);

		mirrorRightButton = new Button("Mirror from right");
		mirrorRightButton.addActionListener(this);
		mirrorRightButton.setBounds(500, 0, 120,30);
		mirrorRightButton.setVisible(true);
		buttonContainer.add(mirrorRightButton);
		
		mirrorLeftButton = new Button("Mirror from left");
		mirrorLeftButton.addActionListener(this);
		mirrorLeftButton.setBounds(500, 30, 120,30);
		mirrorLeftButton.setVisible(true);
		buttonContainer.add(mirrorLeftButton);		

		copyButton = new Button("Copy Expression");
		copyButton.addActionListener(this);
		copyButton.setBounds(500, 60, 120,30);
		copyButton.setVisible(true);
		buttonContainer.add(copyButton);

		pasteButton = new Button("Paste Expression");
		pasteButton.addActionListener(this);
		pasteButton.setBounds(500, 90, 120,30);
		pasteButton.setVisible(true);
		buttonContainer.add(pasteButton);
		
		Button saveButton = new Button("Save");
		saveButton.addActionListener(this);
		saveButton.setBounds(500, 120, 60, 30);
		saveButton.setVisible(true);
		buttonContainer.add(saveButton);
		
		add(buttonContainer);

		emotionContainer = new Container();
		emotionContainer.setBounds(WindowWidth-200, 0, 200, WindowHeight-200);
		emotionContainer.setVisible(true);

		excursionContainer = new Container();
		excursionContainer.setBounds(700, WindowHeight-190, 700, 190);
		excursionContainer.setVisible(true);
		
		numberContainer = new Container();
		numberContainer.setBounds(WindowWidth-200, WindowHeight-230, 200, 230);
		numberContainer.setVisible(true);

		ButtonGroup emotionGroup = new ButtonGroup();			
		ButtonGroup excursionGroup = new ButtonGroup();			
		ButtonGroup percentAGroup = new ButtonGroup();			
		ButtonGroup percentBGroup = new ButtonGroup();			
		int y1 = 0;
		int y2 = 0;
		int x2 = 0;
		for (int i=0; (i<faceDisplay.getExpressions().size()); ++i) {
			Expression ex = faceDisplay.getExpression(i);
			JRadioButton testButton = new JRadioButton(ex.getName());
			testButton.addActionListener(this);
			testButton.setVisible(true);
			testButton.setBackground(Color.white);
			if (ex.getAttack()>0) { // this must be an excursion
				testButton.setBounds(x2, y2, 120, 22);
				excursionButtons.add(testButton);
				excursionGroup.add(testButton);
				excursionContainer.add(testButton);
				y2+=20;
				if (((i % 5) == 0) & (i>5)) {
					y2 = 0;
					x2 += 120;
				}
			}
			else { // this must be a regular expression
				testButton.setBounds(0, y1, 200,22);
				if (y1==0) // special case: null expression should not be altered
					testButton.setBackground(Color.lightGray);
				emotionButtons.add(testButton);
				emotionGroup.add(testButton);
				emotionContainer.add(testButton);
				y1+=20;
			}
		}
		y2=20;
		for (int i=0; (i<10); ++i) {
			JRadioButton testButtonA = new JRadioButton(String.valueOf(i+1)+"0%");
			testButtonA.addActionListener(this);
			testButtonA.setVisible(true);
			testButtonA.setBackground(Color.white);
			testButtonA.setBounds(0, y2, 100, 18);
			percentAButtons.add(testButtonA);
			percentAGroup.add(testButtonA);
			numberContainer.add(testButtonA);
			JRadioButton testButtonB = new JRadioButton(String.valueOf(i+1)+"0 %");
			testButtonB.addActionListener(this);
			testButtonB.setVisible(true);
			testButtonB.setBackground(Color.white);
			testButtonB.setBounds(100, y2, 100, 18);
			percentBButtons.add(testButtonB);
			percentBGroup.add(testButtonB);
			numberContainer.add(testButtonB);
			y2+=18;
		}
		
		emotionButtons.get(0).setSelected(true);
		excursionButtons.get(0).setSelected(true);
		percentAButtons.get(9).setSelected(true);
		percentBButtons.get(9).setSelected(true);
		add(emotionContainer);
		add(excursionContainer);
		add(numberContainer);
		
		displayExpression = faceDisplay.getExpression(nullExpression).clone();	
		displayExpression.setName("display");
		
		attackTimer=new Timer(20, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isAnimation = true;
				int duration = faceDisplay.getExpression(iExcursion).getAttack();
				float x = ((float)animationStep)/duration;
				displayExpression.buildExpression(faceDisplay.getExpression(nullExpression), 
						faceDisplay.getExpression(iEmotion), emoFraction, 
						faceDisplay.getExpression(iExcursion), excFraction, x);
				++animationStep;
				if (animationStep>duration) {
					attackTimer.stop();
					sustainTimer.start();
					animationStep = 0;
				}
				repaint();
			};
		});
		attackTimer.setRepeats(true);

		sustainTimer=new Timer(20, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isAnimation = true;
				int duration = faceDisplay.getExpression(iExcursion).getSustain();
				++animationStep;
				if (animationStep>duration) {
					sustainTimer.stop();
					decayTimer.start();
					animationStep = faceDisplay.getExpression(iExcursion).getDecay();
				}
			};
		});
		sustainTimer.setRepeats(true);
		
		decayTimer=new Timer(20, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isAnimation = true;
				int duration = faceDisplay.getExpression(iExcursion).getDecay();
				float x = ((float)animationStep)/duration;
				displayExpression.buildExpression(faceDisplay.getExpression(nullExpression), 
						faceDisplay.getExpression(iEmotion), emoFraction, 
						faceDisplay.getExpression(iExcursion), excFraction, x);
				--animationStep;
				if (animationStep<0) {
					decayTimer.stop();
					intervalTimer.start();
				}
				repaint();
			};
		});
		decayTimer.setRepeats(true);
		
		irisTimer=new Timer(100, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isAnimation = true;
				repaint();
			};
		});
		irisTimer.setRepeats(true);
//		irisTimer.start();
		
		intervalTimer=new Timer(2000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				animationStep = 0;
				attackTimer.start();
			};
		});
		intervalTimer.setRepeats(false);

	}
// ************************************************************
	// A conventional Action handler
	public void actionPerformed(ActionEvent e) {
		attackTimer.stop();
		sustainTimer.stop();
		decayTimer.stop();
		intervalTimer.stop();
		boolean gotcha = false;
		if (e.getActionCommand().equals("Mirror from right")) {
			mirrorExpression(true);
			gotcha = true;
		}
		if (e.getActionCommand().equals("Mirror from left")) {
			mirrorExpression(false);
			gotcha = true;
		}

		if (e.getActionCommand().equals("Copy Expression")) {
			copiedExpression = iExpression;
			gotcha = true;
		}
		
		if (e.getActionCommand().equals("Paste Expression")) {
			gotcha = true;
			Expression target = faceDisplay.getExpression(iExpression);
			Expression source = faceDisplay.getExpression(copiedExpression);
			for (int i=0; (i<Expression.FeatureCount); ++i) {
				target.getFeature(i).setSize(source.getFeature(i).getSize());
				for (int j=0; (j<target.getFeature(i).getSize()); ++j) {
					target.getFeature(i).setRightX(j, source.getFeature(i).getRightX(j));
					target.getFeature(i).setRightY(j, source.getFeature(i).getRightY(j));
					target.getFeature(i).setLeftX(j, source.getFeature(i).getLeftX(j));
					target.getFeature(i).setLeftY(j, source.getFeature(i).getLeftY(j));
					target.getFeature(i).setRightBigX(j, source.getFeature(i).getRightBigX(j));
					target.getFeature(i).setRightBigY(j, source.getFeature(i).getRightBigY(j));
					target.getFeature(i).setLeftBigX(j, source.getFeature(i).getLeftBigX(j));
					target.getFeature(i).setLeftBigY(j, source.getFeature(i).getLeftBigY(j));					
				}
			}
			repaint();
		}
		
		if (e.getActionCommand().equals("Save")) {
			gotcha = true;
			XMLHandler.saveExpressions(faceDisplay);
		}
		else {
			int i = 0;
			while ((i<emotionButtons.size()) & !gotcha) {
				if (e.getActionCommand().equals(emotionButtons.get(i).getText())) {
					gotcha = true;
					iEmotion = i;
					iMagEmotion = 10;
					emoFraction = 1.0f;
					iFeature = 0;
					iPoint = 0;
					percentAButtons.get(9).setSelected(true);
					checkForAnimation(true, e.getActionCommand());
					displayExpression.buildExpression(faceDisplay.getExpression(nullExpression), 
							faceDisplay.getExpression(iEmotion), emoFraction, 
							faceDisplay.getExpression(iExcursion), excFraction, 0.0f);
					resetLineThicknessButton(displayExpression.getFeature(iFeature));
					repaint();
				}	
				++i;
			}
			i = 0;
			while ((i<excursionButtons.size()) & !gotcha) {
				if (e.getActionCommand().equals(excursionButtons.get(i).getText())) {
					gotcha = true;
					iExcursion = i;
					iMagExcursion = 10;
					excFraction = 1.0f;
					iFeature = 0;
					iPoint = 0;
					percentBButtons.get(9).setSelected(true);
					checkForAnimation(false, e.getActionCommand());
					resetLineThicknessButton(faceDisplay.getExpression(iExpression).getFeature(iFeature));
					displayExpression.buildExpression(faceDisplay.getExpression(nullExpression), 
							faceDisplay.getExpression(iEmotion), emoFraction, 
							faceDisplay.getExpression(iExcursion), excFraction, 1.0f);
					repaint();
				}	
				++i;
			}
			i = 0;
			while ((i<percentAButtons.size()) & !gotcha) {
				if (e.getActionCommand().equals(percentAButtons.get(i).getText())) {
					gotcha = true;
					iMagEmotion = i;
					emoFraction = ((float)iMagEmotion)/10.0f;
					displayExpression.buildExpression(faceDisplay.getExpression(nullExpression), 
							faceDisplay.getExpression(iEmotion), emoFraction, 
							faceDisplay.getExpression(iExcursion), excFraction, 0.0f);
					setupAnimation();
					repaint();
				}	
				++i;
			}
			i = 0;
			while ((i<percentBButtons.size()) & !gotcha) {
				if (e.getActionCommand().equals(percentBButtons.get(i).getText())) {
					gotcha = true;
					iMagExcursion = i;
					excFraction = ((float)iMagExcursion)/10.0f;
					displayExpression.buildExpression(faceDisplay.getExpression(nullExpression), 
							faceDisplay.getExpression(iEmotion), emoFraction, 
							faceDisplay.getExpression(iExcursion), excFraction, 1.0f);
					setupAnimation();
					repaint();
				}	
				++i;
			}
			i = 0;
			while ((i<lineThicknessButtons.size()) & !gotcha) {
				if (e.getActionCommand().equals(lineThicknessButtons.get(i).getText())) {
					gotcha = true;
					iLineThickness = i;
					Feature f = faceDisplay.getExpression(iExpression).getFeature(iFeature);
					if (isRightSide)
						f.setRightLineThickness(iPoint, i);
					else 
						f.setLeftLineThickness(iPoint, i);
					displayExpression.buildExpression(faceDisplay.getExpression(nullExpression), 
							faceDisplay.getExpression(iEmotion), emoFraction, 
							faceDisplay.getExpression(iExcursion), excFraction, 0.0f);
					repaint();
				}	
				++i;
			}
		}
	}
// ************************************************************
	void resetLineThicknessButton(Feature f) {
		for (int i = 0; (i < 6); ++i) 
			lineThicknessButtons.get(i).setEnabled(true);				
		iLineThickness = -1;
		boolean gotcha = false;
		while ((iLineThickness < 6) & !gotcha) {
			++iLineThickness;
			if (isRightSide) 
				gotcha = (iLineThickness == f.getRightLineThickness(iPoint));
			else 
				gotcha = (iLineThickness == f.getLeftLineThickness(iPoint));
		}
		if (gotcha)
			lineThicknessButtons.get(iLineThickness).setSelected(true);		
		else {
			if (isRightSide)
				System.out.println("Error! Out-of-range LineThickness: "+f.getRightLineThickness(iPoint));
			else
				System.out.println("Error! Out-of-range LineThickness: "+f.getLeftLineThickness(iPoint));
		}
	}
// ************************************************************
	void checkForAnimation(boolean isEmotion, String actionCommand) {
		iExpression = -1;
		boolean gotcha = false;
		while (!gotcha & (iExpression<faceDisplay.getExpressions().size())) {
			++iExpression;
			gotcha = actionCommand.equals(faceDisplay.getExpression(iExpression).getName());
		}
		if (isEmotion)
			iEmotion = iExpression;
		else
			iExcursion = iExpression;
		setupAnimation();
	}
// ************************************************************
	void setupAnimation() {
		// This sets us up for animation in the special case in which 
		// both an emotion and an excursion have been selected
		if ((iEmotion>0) & (iExcursion>1)) {
//			iExpression = clipBoardExpression;
			attackTimer.start();
			animationStep = 0;
		}
		/*
		else {
			if ((iEmotion>0) & (iExcursion<=1))
				iExpression = iEmotion;
			else
				iExpression = iExcursion;
		}
	*/	
	}
// ************************************************************
	void mirrorExpression(boolean isFromRight) {
		Feature tf = faceDisplay.getExpression(iExpression).getFeature(iFeature);
		for (int i=0; (i<tf.getSize()); ++i) {
			if (isFromRight) {
				tf.setLeftX(i, tf.getRightX(i));
				tf.setLeftY(i, tf.getRightY(i));
				tf.setLeftBigX(i, tf.getRightBigX(i));
				tf.setLeftBigY(i, tf.getRightBigY(i));
				tf.setLeftLineThickness(i, tf.getRightLineThickness(i));
			}
			else {
				tf.setRightX(i, tf.getLeftX(i));
				tf.setRightY(i, tf.getLeftY(i));
				tf.setRightBigX(i, tf.getLeftBigX(i));
				tf.setRightBigY(i, tf.getLeftBigY(i));
				tf.setRightLineThickness(i, tf.getLeftLineThickness(i));
			}
		}
		repaint();
	}
// ************************************************************
	public void itemStateChanged(ItemEvent e) {
		isLabelled = labelCheckBox.getState();
//		repaint();
	}
// ************************************************************
	// These are the control points that are used to edit facial expressions.
	// Many features must have their own special-case drawing code.
	public void drawControlRects() {
		backBuffer.setColor(Color.black);
		backBuffer.setFont(numeralFont);
		Expression theExpression = faceDisplay.getExpression(iExpression);
		for (int j=0; j<Expression.FeatureCount; ++j) {
			Feature fe = theExpression.getFeature(j);
			int fSize = fe.getSize();
			// the UpperLip crosses the centerline, so they are drawn differently
			if (!fe.getLabel().equals("UpperLip")) {
				for (int i=0; (i<fSize); ++i) {
					if (i>0) {
						backBuffer.drawLine(fe.getRightBigX(i-1)+4, fe.getRightBigY(i-1)+4,fe.getRightBigX(i)+4,fe.getRightBigY(i)+4);
						backBuffer.drawLine(fe.getLeftBigX(i-1)+4, fe.getLeftBigY(i-1)+4,fe.getLeftBigX(i)+4,fe.getLeftBigY(i)+4);
					}
					if ((j!=iFeature) | (i!=iPoint)) {
						backBuffer.drawRect(fe.getRightBigX(i), fe.getRightBigY(i), 8, 8);
						backBuffer.drawRect(fe.getLeftBigX(i), fe.getLeftBigY(i), 8, 8);
					}
					else {
						if (isRightSide) {
							backBuffer.fillRect(fe.getRightBigX(i), fe.getRightBigY(i), 8, 8);
							backBuffer.drawRect(fe.getLeftBigX(i), fe.getLeftBigY(i), 8, 8);
						}
						else {
							backBuffer.fillRect(fe.getLeftBigX(i), fe.getLeftBigY(i), 8, 8);	
							backBuffer.drawRect(fe.getRightBigX(i), fe.getRightBigY(i), 8, 8);
						}
					}
					if (isLabelled) {
						backBuffer.drawString(String.valueOf(i),fe.getRightBigX(i)+2, fe.getRightBigY(i)-2);
						backBuffer.drawString(String.valueOf(i),fe.getLeftBigX(i)+2, fe.getLeftBigY(i)-2);
					}						
				}
			}
			
			if (fe.getLabel().equals("Eye")) { // close the eye polygon
				backBuffer.drawLine(fe.getRightBigX(fSize-1)+4, fe.getRightBigY(fSize-1)+4,fe.getRightBigX(0)+4,fe.getRightBigY(0)+4);
				backBuffer.drawLine(fe.getLeftBigX(fSize-1)+4, fe.getLeftBigY(fSize-1)+4,fe.getLeftBigX(0)+4,fe.getLeftBigY(0)+4);
			}
			
			if (fe.getLabel().equals("UpperLip")) {
				for (int i=0; (i<fSize); ++i) {
					if (i>0)
						backBuffer.drawLine(fe.getRightBigX(i-1)+4, fe.getRightBigY(i-1)+4,fe.getRightBigX(i)+4,fe.getRightBigY(i)+4);
					backBuffer.drawRect(fe.getRightBigX(i), fe.getRightBigY(i), 8, 8);
					if (isLabelled)
						backBuffer.drawString(String.valueOf(i),fe.getRightBigX(i)+2, fe.getRightBigY(i)-2);
				}
				
				// draw the connector between left and right bottom lines
				backBuffer.drawLine(fe.getRightBigX(fSize-1)+4, fe.getRightBigY(fSize-1)+4,fe.getLeftBigX(fSize-1)+4,fe.getLeftBigY(fSize-1)+4);
				for (int i=fSize-1; (i>=0); --i) {
					if (i>0)
						backBuffer.drawLine(fe.getLeftBigX(i-1)+4, fe.getLeftBigY(i-1)+4,fe.getLeftBigX(i)+4,fe.getLeftBigY(i)+4);
					backBuffer.drawRect(fe.getLeftBigX(i), fe.getLeftBigY(i), 8, 8);
					if (isLabelled)
						backBuffer.drawString(String.valueOf(i),fe.getLeftBigX(i)+2, fe.getLeftBigY(i)-2);
				}
				// draw the connector between left and right top lines
				backBuffer.drawLine(fe.getLeftBigX(0)+4, fe.getLeftBigY(0)+4,fe.getRightBigX(0)+4,fe.getRightBigY(0)+4);
			}

			// Yes, I could have made this an else-clause for line 366, but I think this is clearer
			if ((fe.getLabel().equals("UpperLip")) | (fe.getLabel().equals("LowerLip"))) {
				for (int i=0; (i<fSize); ++i) {
					if (i>0)
						backBuffer.drawLine(fe.getRightBigX(i-1)+4, fe.getRightBigY(i-1)+4,fe.getRightBigX(i)+4,fe.getRightBigY(i)+4);
					backBuffer.drawRect(fe.getRightBigX(i), fe.getRightBigY(i), 8, 8);
					if (isLabelled)
						backBuffer.drawString(String.valueOf(i),fe.getRightBigX(i)+2, fe.getRightBigY(i)-2);
				}
				// draw the connector between left and right bottom lines
				backBuffer.drawLine(fe.getRightBigX(fSize-1)+4, fe.getRightBigY(fSize-1)+4,fe.getLeftBigX(fSize-1)+4,fe.getLeftBigY(fSize-1)+4);
				for (int i=fSize-1; (i>=0); --i) {
					if (i>0)
						backBuffer.drawLine(fe.getLeftBigX(i-1)+4, fe.getLeftBigY(i-1)+4,fe.getLeftBigX(i)+4,fe.getLeftBigY(i)+4);
					backBuffer.drawRect(fe.getLeftBigX(i), fe.getLeftBigY(i), 8, 8);
					if (isLabelled)
						backBuffer.drawString(String.valueOf(i),fe.getLeftBigX(i)+2, fe.getLeftBigY(i)-2);
				}
				// draw the connector between left and right top lines
				backBuffer.drawLine(fe.getLeftBigX(0)+4, fe.getLeftBigY(0)+4,fe.getRightBigX(0)+4,fe.getRightBigY(0)+4);
			}			
		}
	}
// ************************************************************
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
	   backBuffer.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		backBuffer.setColor(Color.white);
		backBuffer.fillRect(0, 0, WindowWidth-250, WindowHeight-200);
		backBuffer.setColor(Color.black);
		backBuffer.setFont(labelFont);

		if (!isAnimation & (draggedPoint == null)) {
			g2.setClip(0,0,WindowWidth,WindowHeight);
			for (int i=0; (i<emotionButtons.size()); ++i)
				emotionButtons.get(i).repaint();					
			for (int i=0; (i<excursionButtons.size()); ++i)
				excursionButtons.get(i).repaint();	
			for (int i=0; (i<percentAButtons.size()); ++i) {
				percentAButtons.get(i).repaint();	
				percentBButtons.get(i).repaint();	
			}
			for (int i=0; (i<lineThicknessButtons.size()); ++i)
				lineThicknessButtons.get(i).repaint();	
			
			backBuffer.drawString("line thickness", 20, WindowHeight-150);
			
			backBuffer.drawString("display partial", WindowWidth-160, WindowHeight-220);
			backBuffer.drawString("emotion              excursion", WindowWidth-190, WindowHeight-200);
		}
		else {
			isAnimation = false;
			g2.setClip(0,0,WindowWidth-200,WindowHeight-200);
		}
		
		// first case: emotion is selected, or excursion selected, but not both
		// result: editable expression
		if ((iEmotion>1) ^ (iExcursion>1)) {
			Feature.setUpFeatures(backBuffer, faceDisplay.getFace(0));
			faceDisplay.getExpression(iExpression).drawBig();
	
			Feature.setUpFeatures(backBuffer, faceDisplay.getFace(1));
			backBuffer.drawImage(faceDisplay.getFace(1).getSmallFace(),750,30,transparent,this);
			displayExpression.drawSmall(750,30);
	
			Feature.setUpFeatures(backBuffer, faceDisplay.getFace(2));
			backBuffer.drawImage(faceDisplay.getFace(2).getSmallFace(),1100,30,transparent,this);
			displayExpression.drawSmall(1100,30);
	
			Feature.setUpFeatures(backBuffer, faceDisplay.getFace(3));
			backBuffer.drawImage(faceDisplay.getFace(3).getSmallFace(),750,430,transparent,this);
			displayExpression.drawSmall(750,430);
	
			Feature.setUpFeatures(backBuffer, faceDisplay.getFace(4));
			backBuffer.drawImage(faceDisplay.getFace(4).getSmallFace(),1100,430,transparent,this);
			displayExpression.drawSmall(1100,430);
			
			Feature.setUpFeatures(backBuffer, faceDisplay.getFace(5));
			backBuffer.drawImage(faceDisplay.getFace(5).getSmallFace(),1450,430,transparent,this);
			displayExpression.drawSmall(1450,430);
			
			drawControlRects();
		}
		
		// second case; both expression and excursion selected
		// result: animation but no editing
		if ((iEmotion>1) & (iExcursion>1)) {

			backBuffer.drawRect(830, 190, 440, 330);
			
//			Feature.setUpFeatures(backBuffer, faceDisplay.getFace(1));
//			backBuffer.drawImage(faceDisplay.getFace(1).getSmallFace(),750,30,transparent,this);
//			displayExpression.drawSmall(750,30);
			
			Feature.setUpFeatures(backBuffer, faceDisplay.getFace(2));
			backBuffer.drawImage(faceDisplay.getFace(2).getSmallFace(),900,200,transparent,this);
			displayExpression.drawSmall(900,200);
	
//			Feature.setUpFeatures(backBuffer, faceDisplay.getFace(3));
//			backBuffer.drawImage(faceDisplay.getFace(3).getSmallFace(),750,430,transparent,this);
//			displayExpression.drawSmall(750,430);
	
//			Feature.setUpFeatures(backBuffer, faceDisplay.getFace(4));
//			backBuffer.drawImage(faceDisplay.getFace(4).getSmallFace(),1100,430,transparent,this);
//			displayExpression.drawSmall(1100,430);
		}
		
		// fourth case: neither expression nor excursion selected
		g2.drawImage(offscreen,0,0,this);
	}
// ************************************************************
	static void launch(String args[]) {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			UIManager.put("Button.foreground",new ColorUIResource(Color.black));
		} catch (Exception evt) {
		}
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
		      public void uncaughtException(Thread t, Throwable e) {
		    	  e.printStackTrace();
		      }
		    });
		Runtime.getRuntime().addShutdownHook(new Thread(){
		});
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				new FaceEditor();		
			}
		});
	}
// ************************************************************
	public static void main(String[] args) {
		System.setProperty("apple.awt.antialiasing", "on");
		System.setProperty("apple.awt.textantialiasing", "on");
		FaceEditor.launch(args);
	}
}
