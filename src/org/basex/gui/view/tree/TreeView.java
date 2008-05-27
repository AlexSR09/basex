package org.basex.gui.view.tree;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.basex.data.Data;
import org.basex.data.Nodes;
import org.basex.gui.GUI;
import org.basex.gui.GUIConstants;
import org.basex.gui.GUIFS;
import org.basex.gui.GUIProp;
import org.basex.gui.layout.BaseXBar;
import org.basex.gui.layout.BaseXLayout;
import org.basex.gui.layout.BaseXPopup;
import org.basex.gui.view.View;
import org.basex.gui.view.ViewData;
import org.basex.query.fs.FSUtils;
import org.basex.util.Array;
import org.basex.util.Performance;

/**
 * This view offers a tree visualization of the database contents.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class TreeView extends View {
  /** References closed nodes. */
  boolean[] opened;
  /** Line Height. */
  int lineH;
  /** Focused tree position. */
  int focusedPos;

  /** Closed Box. */
  private BufferedImage closedBox;
  /** Opened Box. */
  private BufferedImage openedBox;
  /** Empty Box. */
  private BufferedImage emptyBox;

  /** Currently marked position. */
  private int mpos;
  /** Scroll Bar. */
  private final BaseXBar scroll;

  /** Vertical mouse position. */
  private int totalW;
  /** Start y value. */
  private int startY;
  /** Total tree Height. */
  private int treeH;
  /** Box Size. */
  private int boxW;
  /** Box Margin. */
  private int boxMargin;

  /**
   * Default Constructor.
   * @param help help text
   */
  public TreeView(final byte[] help) {
    super(help);
    createBoxes();
    setMode(GUIConstants.FILL.UP);
    setLayout(new BorderLayout());
    scroll = new BaseXBar(this);
    add(scroll, BorderLayout.EAST);
    popup = new BaseXPopup(this, GUIConstants.POPUP);
  }

  @Override
  public void refreshInit() {
    scroll.pos(0);

    if(!GUI.context.db()) {
      opened = null;
    } else if(GUIProp.showtree) {
      refreshOpenedNodes();
      refreshHeight();
      repaint();
    }
  }

  /**
   * Refreshes opened nodes.
   */
  private void refreshOpenedNodes() {
    final Data data = GUI.context.data();
    opened = new boolean[data.size];
    final int is = data.size;
    for(int pre = 0; pre < is; pre++) {
      opened[pre] = data.parent(pre, data.kind(pre)) <= 0;
    }
  }

  @Override
  public void refreshFocus() {
    repaint();
  }

  @Override
  public void refreshMark() {
    final int pre = focused;
    if(pre == -1) return;

    // jump to the currently marked node
    final Data data = GUI.context.data();
    final int par = data.parent(pre, data.kind(pre));
    // open node if it's not visible
    jumpTo(pre, par != -1 && !opened[par]);
    repaint();
  }

  @Override
  public void refreshContext(final boolean more, final boolean quick) {
    if(!GUIProp.showtree) return;

    startY = 0;
    scroll.pos(0);

    if(more) jumpTo(GUI.context.current().pre[0], true);
    refreshHeight();
    repaint();
  }

  @Override
  public void refreshLayout() {
    createBoxes();
    if(opened == null) return;
    refreshOpenedNodes();
    refreshHeight();
    repaint();
  }

  @Override
  public void refreshUpdate() {
    if(opened == null) return;
    final Data data = GUI.context.data();
    if(opened.length < data.size) opened = Array.finish(opened, data.size);

    startY = 0;
    scroll.pos(0);

    final Nodes marked = GUI.context.marked();
    if(marked.size != 0) jumpTo(marked.pre[0], true);
    refreshHeight();
    repaint();
  }

  /**
   * Refreshes tree height.
   */
  void refreshHeight() {
    if(opened == null) return;

    treeH = new TreeIterator(this).height();
    scroll.height(treeH + 5);
  }

  @Override
  public void paintComponent(final Graphics g) {
    if(opened == null) {
      refreshInit();
      return;
    }

    super.paintComponent(g);
    if(opened == null) return;
    BaseXLayout.antiAlias(g);

    painting = true;
    startY = -scroll.pos();
    totalW = getWidth() - (treeH > getHeight() ? scroll.getWidth() : 0);

    mpos = 0;
    final TreeIterator it = new TreeIterator(this, startY + 5, getHeight());
    final Data data = GUI.context.data();
    while(it.more()) {
      final int kind = data.kind(it.pre);
      final boolean elem = kind == Data.ELEM || kind == Data.DOC;
      final int x = 8 + it.level * (lineH >> 2) + (elem ? lineH : boxW);
      drawString(g, it.pre, x, it.y + boxW);
    }
    painting = false;
  }

  /**
   * Draws a string and checks mouse position.
   * @param g graphics reference
   * @param pre pre value
   * @param x horizontal coordinate
   * @param y vertical coordinate
   */
  void drawString(final Graphics g, final int pre, final int x, final int y) {
    final Data data = GUI.context.data();
    final Nodes marked = GUI.context.marked();

    final int kind = data.kind(pre);
    final boolean elem = kind == Data.ELEM || kind == Data.DOC;

    while(mpos < marked.size && marked.pre[mpos] < pre) mpos++;

    Color col = Color.black;
    Font fnt = GUIConstants.font;
    if(mpos < marked.size && marked.pre[mpos] == pre) {
      // mark node
      col = GUIConstants.colormark3;
      fnt = GUIConstants.bfont;
    }
    if(y < -lineH) return;

    g.setColor(GUIConstants.color2);
    g.drawLine(2, y + boxMargin - 1, totalW - 5, y + boxMargin - 1);

    final byte[] name = ViewData.content(data, pre, false);
    final boolean file = data.deepfs && FSUtils.isFile(data, pre);
    final boolean dir = data.deepfs && FSUtils.isDir(data, pre);

    int p = focused;
    while(p > pre) p = ViewData.parent(data, p);
    if(pre == p) {
      g.setColor(GUIConstants.color3);
      g.fillRect(0, y - boxW - boxMargin, totalW, lineH + 1);
    }
    int xx = x;
    final int yy = y;

    if(elem) {
      if(data.deepfs) {
        // print file icon
        Image img = null;
        if(file) {
          img = GUIFS.images(name, 0);
        } else if(dir) {
          img = opened[pre] ? GUIFS.folder2[0] : GUIFS.folder1[0];
        } else {
          img = opened[pre] ? openedBox : closedBox;
        }
        g.drawImage(img, xx - lineH, yy - boxW - 1, this);
        if(file || dir) xx += 5;
      } else {
        final Image box = opened[pre] ? openedBox : closedBox;
        g.drawImage(box, xx - lineH, yy - boxW - 1, this);
      }
    }

    g.setFont(fnt);
    g.setColor(col);

    int tw = totalW + 6;
    if(file && tw - xx > 140) {
      final long size = FSUtils.getSize(data, pre);
      final String text = Performance.formatSize(size, false);
      tw -= BaseXLayout.width(g, text) + 10;
      g.drawString(text, tw, yy);
    }
    BaseXLayout.chopString(g, name, xx, yy - GUIProp.fontsize, tw - xx - 10);

    if(focused == pre) {
      g.setColor(GUIConstants.color6);
      g.drawRect(1, yy - boxW - boxMargin, totalW - 3, lineH + 1);
      g.drawRect(2, yy - boxW - boxMargin + 1, totalW - 5, lineH - 1);
    }
  }

  /**
   * Focuses the current pre value.
   * @param x x mouse position
   * @param y y mouse position
   * @return currently focused id
   */
  private boolean focus(final int x, final int y) {
    if(opened == null) return false;

    final TreeIterator it = new TreeIterator(this, startY + 3, getHeight());
    final Data data = GUI.context.data();
    while(it.more()) {
      if(y > it.y && y <= it.y + lineH) {
        Cursor c = GUIConstants.CURSORARROW;
        final int kind = data.kind(it.pre);
        if(kind == Data.ELEM || kind == Data.DOC) {
          // set cursor when moving over tree boxes
          final int xx = 8 + it.level * (lineH >> 2) + lineH;
          if(x > xx - 20 && x < xx) c = GUIConstants.CURSORHAND;
        }
        GUI.get().cursor(c);
        notifyFocus(it.pre, this);
        repaint();
        return true;
      }
    }
    return false;
  }

  /**
   * Jumps to the specified pre value.
   * @param pre pre value to be found
   * @param open opened folder
   */
  void jumpTo(final int pre, final boolean open) {
    if(getWidth() == 0 || !GUIProp.showtree) return;

    if(open) {
      int p = pre;
      while(p != 0) {
        opened[p] = true;
        p = ViewData.parent(GUI.context.data(), p);
      }
      refreshHeight();
    }

    // find specified pre value
    final TreeIterator it = new TreeIterator(this);
    while(it.more() && pre != it.pre);

    // set new vertical position
    final int y = -it.y;
    final int h = getHeight();
    if(y > startY || y + h < startY + lineH) {
      startY = Math.min(0, Math.max(-treeH + h - 5, y + lineH));
      scroll.pos(-startY);
    }
  }

  /**
   * Creates click boxes.
   */
  private void createBoxes() {
    final int s = GUIProp.fontsize;
    boxMargin = s >> 2;
    lineH = s + boxMargin;
    boxW = s - boxMargin;
    final int sp = Math.max(1, s >> 4);

    emptyBox = new BufferedImage(boxW + 1, boxW + 1,
        BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = emptyBox.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(GUIConstants.color6);
    g.fillOval((boxW >> 2) - 1, (boxW >> 2) + 1, boxW >> 1, boxW >> 1);
    g.setColor(GUIConstants.color4);
    g.fillOval((boxW >> 2) - 2, boxW >> 2, boxW >> 1, boxW >> 1);

    openedBox = new BufferedImage(boxW + 1, boxW + 1,
        BufferedImage.TYPE_INT_ARGB);
    g = openedBox.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);

    Polygon p = new Polygon(new int[] { 0, boxW, boxW >> 1 }, new int[] {
        boxW - sp >> 1, boxW - sp >> 1, boxW }, 3);
    p.translate(0, -1);
    g.setColor(GUIConstants.color6);
    g.fillPolygon(p);
    p.translate(-1, -1);
    g.setColor(GUIConstants.color4);
    g.fillPolygon(p);

    closedBox = new BufferedImage(boxW + 1, boxW + 1,
        BufferedImage.TYPE_INT_ARGB);
    g = closedBox.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);

    p = new Polygon(new int[] { boxW - sp >> 1, boxW, boxW - sp >> 1 },
        new int[] { 0, boxW >> 1, boxW }, 3);
    p.translate(-1, 1);
    g.setColor(GUIConstants.color6);
    g.fillPolygon(p);
    p.translate(-1, -1);
    g.setColor(GUIConstants.color4);
    g.fillPolygon(p);
  }

  @Override
  public void mouseMoved(final MouseEvent e) {
    if(working) return;
    super.mouseMoved(e);
    // set new focus
    focus(e.getX(), e.getY());
  }

  @Override
  public void mousePressed(final MouseEvent e) {
    super.mousePressed(e);
    if(working || opened == null) return;

    if(!focus(e.getX(), e.getY())) return;

    final boolean left = SwingUtilities.isLeftMouseButton(e);
    final int pre = focused;

    // add or remove marked node
    final Nodes marked = GUI.context.marked();
    if(!left) {
      if(marked.find(pre) < 0) notifyMark(0);
    } else if(getCursor() == GUIConstants.CURSORHAND) {
      // open/close entry
      opened[pre] ^= true;
      refreshHeight();
      repaint();
    } else if(e.getClickCount() == 2) {
      notifyContext(marked, false);
    } else if(e.isShiftDown()) {
      notifyMark(1);
    } else if(e.isControlDown()) {
      notifyMark(2);
    } else {
      if(marked.find(pre) < 0) notifyMark(0);
    }
  }

  @Override
  public void mouseClicked(final MouseEvent e) {
    if(!SwingUtilities.isLeftMouseButton(e) || working || opened == null)
      return;

    // launch a program
    if(getCursor() == GUIConstants.CURSORHAND)
      FSUtils.launch(GUI.context.data(), focused);
  }

  @Override
  public void mouseDragged(final MouseEvent e) {
    final boolean left = SwingUtilities.isLeftMouseButton(e);
    if(!left || working || opened == null) return;
    super.mouseDragged(e);

    // marks currently focused node
    if(focus(e.getX(), e.getY())) notifyMark(1);
  }

  @Override
  public void mouseWheelMoved(final MouseWheelEvent e) {
    if(working) return;
    scroll.pos(scroll.pos() + e.getUnitsToScroll() * 20);
    repaint();
  }

  @Override
  public void keyPressed(final KeyEvent e) {
    super.keyPressed(e);
    if(working || opened == null) return;

    int focus = focusedPos == -1 ? 0 : focusedPos;
    if(focused == -1) focused = 0;
    final int focusPre = focused;
    final Data data = GUI.context.data();
    int kind = data.kind(focusPre);
    int key = e.getKeyCode();

    if(e.isShiftDown() && key == KeyEvent.VK_ENTER && focusPre != -1) {
      // launch file
      FSUtils.launch(data, focused);
    } else if(key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_LEFT) {
      // open/close subtree
      final boolean open = key == KeyEvent.VK_RIGHT;
      final boolean fs = data.deepfs;
      if(e.isShiftDown()) {
        opened[focusPre] = open;
        final int s = data.size;
        for(int pre = focusPre + (fs ? data.attSize(focusPre, kind) : 1);
          pre != s && data.parent(pre, data.kind(pre)) >= focusPre;
          pre += fs ? data.attSize(pre, kind) : 1) {
          opened[pre] = open;
          kind = data.kind(pre);
        }
        refreshHeight();
        repaint();
        return;
      }

      if((open ^ opened[focusPre]) && (!ViewData.isLeaf(data, focusPre) ||
          data.attSize(focusPre, kind) > 1)) {
        opened[focusPre] = open;
        refreshHeight();
        repaint();
      } else {
        key = open ? KeyEvent.VK_DOWN : KeyEvent.VK_UP;
      }
    }
    
    if(key == KeyEvent.VK_DOWN) {
      focus = Math.min(data.size, focus + 1);
    } else if(key == KeyEvent.VK_UP) {
      focus = Math.max(0, focus - 1);
    } else if(key == KeyEvent.VK_PAGE_DOWN) {
      focus = Math.min(data.size - 1, focus + getHeight() / lineH);
    } else if(key == KeyEvent.VK_PAGE_UP) {
      focus = Math.max(0, focus - getHeight() / lineH);
    } else if(key == KeyEvent.VK_HOME) {
      focus = 0;
    } else if(key == KeyEvent.VK_END) {
      focus = data.size - 1;
    }
    if(focus == focusedPos) return;

    // calculate new tree position
    focused = -1;
    final Nodes curr = GUI.context.current();
    int pre = curr.pre[0];
    final TreeIterator it = new TreeIterator(this);
    while(it.more() && focus-- != 0) pre = it.pre;

    if(pre == curr.pre[0] && key == KeyEvent.VK_DOWN) pre++;
    notifyFocus(pre, this);
    jumpTo(pre, false);
    repaint();
  }
}
