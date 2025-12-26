/*
  Copyright 2025 Paul Janssens - All rights reserved

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package be.afront.reader

import EventHandler.{FileSelection, SENSITIVITY, WHEEL_SENSITIVITY, handle, openFromUI, openSelectedFiles}
import CBZImages.Direction.{LeftToRight, RightToLeft}
import state.ReaderState.{Encoding, Help, Mode, Size, modeFrom}
import state.ReaderState.Mode.{Blank, Dual1, Dual1b, Dual2, Single, SingleEvenOdd, SingleOddEven}
import CBZImages.{Dimensions, FileCheck, checkFile}
import ResourceLookup.{MenuItemKey, MenuKey}
import menu.MenuBuilder.{alterMenu, menuItem, modeMenuItems}
import EventHandler.FileSelection.{Event, Restore, UI}
import state.RecentStates.EMPTY
import state.{AggregatePersistedState, PartialState, ReaderState, RecentState, RecentStates}
import state.Preferences.PreferenceKey
import menu.{RecentFileMenuItem, TaggedMenuBar}

import java.awt.desktop.{OpenFilesEvent, OpenFilesHandler}
import java.awt.{CheckboxMenuItem, FileDialog, Frame, Menu, MenuItem, Point}
import java.awt.event.{ActionEvent, ActionListener, ItemEvent, KeyEvent, KeyListener, MouseEvent, MouseListener, MouseMotionListener, MouseWheelEvent, MouseWheelListener}
import javax.swing.{JEditorPane, JFrame, JScrollPane, SwingUtilities}
import java.awt.event.KeyEvent.{VK_2, VK_4, VK_6, VK_8, VK_ADD, VK_DOWN, VK_LEFT, VK_MINUS, VK_NUMPAD2, VK_NUMPAD4, VK_NUMPAD6, VK_NUMPAD8, VK_PLUS, VK_Q, VK_RIGHT, VK_SHIFT, VK_SPACE, VK_SUBTRACT, VK_UP}
import java.awt.event.ItemEvent.SELECTED
import java.io.File
import scala.List
import scala.jdk.CollectionConverters.given
import scala.util.{Failure, Success}


class EventHandler(frame:JFrame, panel1:ImagePanel, panel2:ImagePanel,
                   initialState:ReaderState, screenSize:Dimensions)(using lookup:ResourceLookup)
  extends KeyListener
    with MouseMotionListener
    with MouseWheelListener
    with MouseListener
    with ActionListener
    with OpenFilesHandler {

  private def init():Unit = {
    frame.addKeyListener(this)
    frame.addMouseMotionListener(this)
    frame.addMouseListener(this)
    frame.addMouseWheelListener(this)
  }

  var state:ReaderState = initialState

  private var initialMouseDown: (p:Point, h:Double, v:Double) = null

  init()

  override def keyPressed(e: KeyEvent): Unit = {
    stateMachine(e.getKeyCode, true) match {
      case Some(newState) =>
        updateState(newState)
      case None =>
        frame.dispose()
    }
  }

  override def keyReleased(e: KeyEvent): Unit = {
    stateMachine(e.getKeyCode, false) match {
      case Some(newState) =>
        updateState(newState)
      case None =>
        frame.dispose()
    }
  }

  private def updateState(newState: ReaderState): Unit =
    updateState(newState, false, false)


  private def updateState(newState:ReaderState, afterOpen:Boolean, clearRecents:Boolean): Unit = {
    if(state.mode != newState.mode) {
      layoutChangeFor(newState)
      frame.revalidate()
    }

    val numFiles = newState.partialStates.size

    if(afterOpen || clearRecents)
      updateMenuBar(numFiles, newState)

    if ((afterOpen || state.direction != newState.direction) && numFiles > 0)
        updateTitle(newState.partialNames)

    state = newState

    panel1.setNewState(state)
    if (numFiles == 2 || state.mode == SingleEvenOdd || state.mode == SingleOddEven)
      panel2.setNewState(state)

    SwingUtilities.invokeLater { () =>
      frame.repaint()
    }
  }

  private def updateTitle(newTitle:String):Unit =
    frame.setTitle(newTitle)

  private def updateMenuBar(count:Int, newState:ReaderState)(using lookup: ResourceLookup):Unit = {
    val menuBar = frame.getMenuBar.asInstanceOf[TaggedMenuBar]
    val count = newState.partialStates.size

    menuBar.withSubMenuDo(MenuKey.File, MenuKey.Recent, _.replaceMenuItems(recentFileMenuItems(state.recentStates)))

    menuBar.withMenuDo(MenuKey.Mode, _.replaceMenuItems(modeMenuItems(count, newState.mode)(using this, lookup)))

    menuBar.withMenuDo(MenuKey.Size, alterMenu(_, newState.size))

    menuBar.withMenuDo(MenuKey.Options, optionsMenu => {
      val rtol = optionsMenu.getItem(0).asInstanceOf[CheckboxMenuItem]
      rtol.setState(newState.direction == RightToLeft)
      val pgn = optionsMenu.getItem(1).asInstanceOf[CheckboxMenuItem]
      pgn.setState(newState.showPageNumbers)
    })
  }

  def recentFileMenuItems(recentStates:RecentStates)(using lookup:ResourceLookup): List[MenuItem] = {
    val nonEmpty = recentStates.nonEmpty
    
    val clear = menuItem(MenuItemKey.Clear, nonEmpty)(using this, lookup)
    clear.addActionListener((e: ActionEvent) => clearRecentFiles())

    if(nonEmpty) {
      recentStates.states.map(recentFileMenuItem) ++ List(new MenuItem("-"), clear)
    } else {
      List(clear)
    }
  }

  def restore(state:RecentState):Unit = {
    openFiles(state.files, Restore)
  }

  private def recentFileMenuItem(state:RecentState): MenuItem = {
    val item = new RecentFileMenuItem(state)
    item.addActionListener((e: ActionEvent) => restore(state))
    item 
  }

  private def updateStateForNewFiles(newState: ReaderState):Unit = {
    val updateNewState = newState.copy(recentStates=newState.recentStates.updateWhere(state.files, state.toSave))
    updateState(updateNewState, true, false)
  }

  private def clearRecentStates():Unit = {
    updateState(state.copy(recentStates = state.recentStates.clear), false, true)
  }

  def updatedRecentStates:RecentStates =
    state.recentStates.updateWhere(state.files, state.toSave)

  private def layoutChangeFor(newState:ReaderState): Unit = {
    val oldMode = state.mode
    val newMode = newState.mode

    frame.remove(panel1)
    frame.remove(panel2)
    val panels = visiblePanels(newMode)
    panels.foreach(frame.add)

    if(panels.size == 1)
      frame.setSize(screenSize.width / 2, screenSize.height)
    else if(panels.size == 2)
      frame.setSize(screenSize.width, screenSize.height)
    else frame.setSize(0, 0)   
  }

  private def visiblePanels(mode:Mode):List[ImagePanel] = {
    mode match {
      case Blank => List()
      case Single => List(panel1)
      case SingleOddEven => List(panel1, panel2)
      case SingleEvenOdd => List(panel1, panel2)
      case Dual1 => List(panel1)
      case Dual1b => List(panel2)
      case Dual2 => List(panel1, panel2)
    }
  }

  private def ifNotBlank(r: => Option[ReaderState]):Option[ReaderState] =
    if(state.mode == Blank) Some(state) else r   
  
  private def stateMachine(keyCode: Int, pressed:Boolean): Option[ReaderState] = {
    if (pressed) keyCode match {
      case VK_SPACE => ifNotBlank(Some(state.nextPage))

      case VK_RIGHT => ifNotBlank(Some(state.right))
      case VK_LEFT => ifNotBlank(Some(state.left))
      case VK_UP => ifNotBlank(Some(state.zoomIn))
      case VK_DOWN => ifNotBlank(Some(state.zoomOut))

      case VK_SHIFT => if(state.mode==Dual1) Some(state.setMode(Dual1b)) else Some(state)

      case VK_MINUS | VK_SUBTRACT => ifNotBlank(Some(state.minus))
      case VK_PLUS | VK_ADD=> ifNotBlank(Some(state.plus))

      case VK_8 | VK_NUMPAD8 => ifNotBlank(Some(state.scrollUp))
      case VK_2 | VK_NUMPAD2 => ifNotBlank(Some(state.scrollDown))
      case VK_4 | VK_NUMPAD4 => ifNotBlank(Some(state.scrollLeft))
      case VK_6 | VK_NUMPAD6 => ifNotBlank(Some(state.scrollRight))

      case VK_Q => None
      case _ => Some(state)
      
    } else keyCode match {
      case VK_SHIFT => if(state.mode==Dual1b) Some(state.setMode(Dual1)) else Some(state)
      case _ => Some(state)
    }
  }

  override def keyTyped(e: KeyEvent): Unit = {}

  override def mousePressed(e: MouseEvent): Unit = {
    initialMouseDown = (e.getPoint, state.hs, state.vs)
  }

  override def mouseReleased(e: MouseEvent): Unit = {
    initialMouseDown = null
  }

  override def mouseDragged(e: MouseEvent): Unit = {
    if(initialMouseDown!=null) {
      // signed for 'normal' dragging, image follows mouse
      val multiplier = SENSITIVITY / state.zoomFactor
      val dx = (initialMouseDown.p.x - e.getX) * multiplier
      val dy = (initialMouseDown.p.y - e.getY) * multiplier
      updateState(state.scrollTo(initialMouseDown.h + dx, initialMouseDown.v + dy))
    } else {
      // initialMouseDown not available for drag event
    }
  }
  
  override def mouseMoved(e: MouseEvent): Unit = { }

  override def mouseClicked(e: MouseEvent): Unit = {
    e.getButton match {
      case MouseEvent.BUTTON1 => updateState(state.nextPage)
      case MouseEvent.BUTTON3 => updateState(state.prevPage)
      case _ =>
    }
  }

  override def mouseEntered(e: MouseEvent): Unit = {}

  override def mouseExited(e: MouseEvent): Unit = {}

  override def actionPerformed(event: ActionEvent): Unit = {
    event.getActionCommand match {
      case MenuItemKey.Open.description => 
        updateStateForNewFiles(openFromUI(state))
      case MenuItemKey.Info.description =>
        displayMetadata()
      case MenuItemKey.Clear.description =>
        clearRecentStates()

      case _ => println("unimplemented command "+ event.getActionCommand)
    }
  }

  private def displayMetadata(): Unit = {
    val dummyOwner = new Frame()
    dummyOwner.setVisible(false)
    val dialog = new InfoDialog(dummyOwner, state.partialStates.map(_.metadata).mkString("\n\n"))
    dialog.setLocationRelativeTo(null)
    dialog.setVisible(true)
    dummyOwner.dispose()
  }

  def directionChange(newState:Int):Unit =
    updateState(state.setDirection(if(newState == SELECTED) RightToLeft else LeftToRight ))

  def togglePageNumbers(newState: Int): Unit =
    updateState(state.setShowPageNumbers(newState == SELECTED))

  def changeMode(newMode:Mode):Unit =
    updateState(state.setMode(newMode))

  def changeSize(newSize:Size):Unit =
    updateState(state.setSize(newSize))

  def changeEncoding(newEncoding: Encoding): Unit =
    updateState(state.setEncoding(newEncoding))

  def changePreference(key:PreferenceKey, newValue:Int):Unit =
    updateState(state.changePreference(key, newValue == SELECTED))

  def displayHelp(help: Help): Unit = {
    val editorPane = new JEditorPane()
    editorPane.setContentType("text/html")
    val url = Option(getClass.getResource("/help/"+help.relativeUrl)).get
    editorPane.setPage(url)
    val scrollPane = new JScrollPane(editorPane)

    val frame = new JFrame(lookup(help))
    frame.add(scrollPane)
    frame.setSize(800, 600)
    frame.setVisible(true)
  }


  override def mouseWheelMoved(e: MouseWheelEvent): Unit =
    updateState(state.scrollVertical(e.getWheelRotation * WHEEL_SENSITIVITY))

  override
  def openFiles(e: OpenFilesEvent): Unit =
    openFiles(e.getFiles.asScala.toList, Event)

  private def clearRecentFiles():Unit =
    updateState(state.copy(recentStates=EMPTY), false, true)

  // only for Event or Restore
  private def openFiles(files:List[File], fileSelection:FileSelection): Unit = {
    if (files.nonEmpty) {
      val images = files.take(2).map(p => checkFile(p, state.encoding)).flatMap((checkResult:FileCheck) => checkResult match {
        case (file, Failure(err)) => handle(file, err);None
        case (file, Success(image)) => Some(image)
      })
      if (images.size == files.size) {
        updateStateForNewFiles(openSelectedFiles(state, fileSelection, images))
      }
    }
  }

  def applicationWillEnd():Unit =
    AppPreferences.save(AggregatePersistedState(state.preferences, updatedRecentStates))
}

object EventHandler {
  val SENSITIVITY = 0.005

  val WHEEL_SENSITIVITY = 0.01

  enum FileSelection {
    case UI, Event, Restore
  }

  private def selectFile(prompt: String, encoding:Encoding): Option[FileCheck] = {
    val dummyFrame = new Frame()
    dummyFrame.setSize(0, 0)

    val dialog = new FileDialog(dummyFrame, prompt, FileDialog.LOAD)
    dialog.setVisible(true)

    val files = dialog.getFiles
    dialog.dispose()
    dummyFrame.dispose()

    if(files.isEmpty) None else Some(checkFile(files(0), encoding))
  }

  @annotation.tailrec
  private def selectFileLoop(prompt:String, encoding:Encoding): Option[CBZImages] = {
    selectFile(prompt, encoding) match {
      case Some((file, Failure(err))) => handle(file, err); selectFileLoop(prompt, encoding)
      case Some((file, Success(image))) => Some(image)
      case None => None
    }
  }

  def handle(file:File, err:Throwable):Unit = {
    val dummyOwner = new Frame()
    dummyOwner.setVisible(false)
    val simpleFileName = file.getName
    val line1 =
      if(err.getCause.isInstanceOf[java.nio.charset.MalformedInputException])
        s"Failed to open \"$simpleFileName\", most likely the current encoding is incorrect" else
        s"Failed to open \"$simpleFileName\""

    val alert = new AlertDialog(dummyOwner, line1, err.getMessage)
    alert.setLocationRelativeTo(null)
    alert.setVisible(true)
    dummyOwner.dispose()
  }

  def openFromUI(state:ReaderState): ReaderState =
    openSelectedFiles(state, UI,
      List(selectFileLoop("select first file", state.encoding), selectFileLoop("select 2nd file", state.encoding)).flatten)

  def openSelectedFiles(currentState:ReaderState, fileSelection:FileSelection, files:List[CBZImages]) : ReaderState = {
    val partialStates = files.map(f => PartialState(f))
    if (fileSelection == Restore) {

      val matchingRecentState = currentState.recentStates.states.find(_.files == files.map(_.file))
      // if there is no match, it means we could not find one or more files. Don't restore partially

      matchingRecentState match {
        case Some(recentState) => {
          val savedState = recentState.save
          new ReaderState(
            savedState.mode,
            partialStates.zip(savedState.currentPages).map((partialState, newPage) => partialState.setPage(newPage)),
            savedState.zoomLevel,
            savedState.hs,
            savedState.vs,
            savedState.size,
            savedState.direction,
            currentState.encoding,
            savedState.showPageNumbers,
            currentState.preferences,
            currentState.recentStates)
        }
        case None => currentState
      }
    } else {
      new ReaderState(partialStates, currentState)
    }
  }
}
