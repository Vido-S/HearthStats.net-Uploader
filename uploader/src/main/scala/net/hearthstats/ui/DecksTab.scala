package net.hearthstats.ui

import java.awt.image.BufferedImage

import grizzled.slf4j.{Logging, Logger}
import net.hearthstats.log.Log
import net.hearthstats.util.Translations.t
import java.io.IOException
import javax.swing._
import net.hearthstats._
import net.miginfocom.swing.MigLayout
import org.json.simple.JSONObject
import zulu.deckexport.extracter.ExtracterMain
import scala.collection.JavaConversions._
import Constants._
import scala.swing.Swing._
import java.awt.BorderLayout
import net.hearthstats.util.HsRobot
import net.hearthstats.util.Browse
import net.hearthstats.util.HsRobot
import scala.Some

class DecksTab(val monitor: Monitor) extends JPanel with Logging {

  val deckSlotComboBoxes = 1 to 9 map { new DeckSlotPanel(_) }

  setLayout(new MigLayout)
  add(new JLabel(" "), "wrap")
  add(new JLabel(t("set_your_deck_slots")), "skip, span 2")
  add(new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Decks-Tab", "Help on Decks tab"), "right,wrap")

  add(new JLabel(" "), "wrap")
  add(deckSlotComboBoxes(0), "skip")
  add(deckSlotComboBoxes(1), "")
  add(deckSlotComboBoxes(2), "wrap")

  add(new JLabel(" "), "wrap")
  add(deckSlotComboBoxes(3), "skip")
  add(deckSlotComboBoxes(4), "")
  add(deckSlotComboBoxes(5), "wrap")

  add(new JLabel(" "), "wrap")
  add(deckSlotComboBoxes(6), "skip")
  add(deckSlotComboBoxes(7), "")
  add(deckSlotComboBoxes(8), "wrap")

  add(new JLabel(" "), "wrap")
  add(new JLabel(" "), "wrap")

  val saveButton = new JButton(t("button.save_deck_slots"))
  saveButton.addActionListener(ActionListener(_ => onEDT(saveDeckSlots())))
  add(saveButton, "skip")

  val refreshButton = new JButton(t("button.refresh"))
  refreshButton.addActionListener(ActionListener(_ =>
    try {
      onEDT(updateDecks())
    } catch {
      case e1: IOException => Main.showErrorDialog("Error updating decks", e1)
    }))
  add(refreshButton, "")

  val exportButton = new JButton(t("button.export_deck"))
  exportButton.addActionListener(ActionListener(_ => onEDT(exportDeck())))
  add(exportButton, "wrap")

  add(new JLabel(" "), "wrap")
  val myDecksButton = new JButton(t("manage_decks_on_hsnet"))
  myDecksButton.addActionListener(ActionListener(_ => Browse(DECKS_URL)))

  add(myDecksButton, "skip,span")

  onEDT(updateDecks())

  def updateDecks() {
    DeckUtils.updateDecks()
    for (d <- deckSlotComboBoxes) d.applyDecks()
  }

  private def name(o: JSONObject): String = {
    Constants.hsClassOptions(Integer.parseInt(o.get("klass_id").toString)) +
      " - " +
      o.get("name").toString.toLowerCase
  }

  private def saveDeckSlots() {
    try {
      val slots = deckSlotComboBoxes map (_.selectedDeckId)
      API.setDeckSlots(slots)
      Main.showMessageDialog(this, API.message)
      updateDecks()
    } catch {
      case e: Exception => Main.showErrorDialog("Error saving deck slots", e)
    }
  }

  private def removeSlot(slot: Int): Unit = {
    if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Are you sure ?", "Remove a deck", JOptionPane.YES_NO_OPTION)) {
      for (i <- slot to 8) {
        val nextIndex = deckSlotComboBoxes(i).comboBox.getSelectedIndex
        deckSlotComboBoxes(i - 1).comboBox.setSelectedIndex(nextIndex)
      }
      deckSlotComboBoxes(8).comboBox.setSelectedIndex(0)
    }
  }

  private def createDeck(in: Option[Deck]): Unit = {
    in match {
      case Some(d) => {
        if (!d.isValid) {
          JOptionPane.showConfirmDialog(this,
            s"""${d.name} is not valid (${d.cardCount} cards). Do you want to edit it first on Hearthstats.net ?""".stripMargin) match {
              case JOptionPane.YES_OPTION =>
                Browse(s"http://hearthstats.net/decks/${d.slug}/edit")
              case JOptionPane.NO_OPTION => doCreate(d)
              case _ =>
            }
        } else doCreate(d)
      }
      case None => {
        JOptionPane.showMessageDialog(this,
          s"""There is no deck in this slot.
             |Choose a deck before pressing Construct""".stripMargin)
      }
    }

    def doCreate(d: Deck) = HsRobot(monitor._hsHelper.getHSWindowBounds).create(d)
  }


  private def exportDeck(): Unit = {
    val hsHelper = monitor._hsHelper
    hsHelper.bringWindowToForeground

    val robot = HsRobot(monitor._hsHelper.getHSWindowBounds)
    robot.collectionScrollAway()

    val img1 = hsHelper.getScreenCapture

    robot.collectionScrollTowards()

    val img2 = hsHelper.getScreenCapture

    val deck = ExtracterMain.exportDeck(img1, img2)

    if (deck == null) {
      Log.info("Could not export deck")
    } else {
      logger.debug("Deck export detected:")
      for (cardString <- deck.toArray) {
        logger.debug(s" - $cardString")
      }
      // This method is just for illustration
      showDebugDeckPopup(deck);
      // deck options should be set here before uploading to server such as deckname, deck class, deck slot, deck owner etc.
      // upload deck to server
    }
  }

  /**
   * This method is available only for illustration.(Can be deleted)
   * @param deck
   */
  def showDebugDeckPopup(deck: zulu.deckexport.card.Deck) {
    val op: JOptionPane = new JOptionPane("Deck export is currently experimental\nand does not yet upload the deck.\nThe deck detected is:", JOptionPane.INFORMATION_MESSAGE)
    val scrollPane: JScrollPane = new JScrollPane
    val list: JList[String] = new JList[String]
    scrollPane.setViewportView(list)
    list.setListData(deck.toArray)
    op.add(scrollPane)
    val dialog: JDialog = op.createDialog(null, "Deck Exporter Info")
    dialog.setAlwaysOnTop(true)
    dialog.setModal(true)
    dialog.setFocusableWindowState(true)
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    dialog.setVisible(true)
  }






  class DeckSlotPanel(slot: Int) extends JPanel {
    setLayout(new BorderLayout)
    add(new JLabel(t("deck_slot.label", slot)), BorderLayout.NORTH)
    val comboBox = new JComboBox[Object]
    add(comboBox, BorderLayout.CENTER)

    val removeBtn = new JButton("x") //TODO use an icon instead
    removeBtn.setToolTipText("Remove this deck and shift next ones")
    removeBtn.addActionListener(ActionListener(_ => removeSlot(slot)))
    add(removeBtn, BorderLayout.EAST)



//
//
//
//    val btnPanel = new JPanel();
//    btnPanel.setLayout(new BorderLayout)
//
//    val buildBtn = new JButton("Build/Upload")
//    buildBtn.setToolTipText("""<html><b>Automatically exports Hearthstone deck into HearthStats website</b><br/>
//    		        <i>You need to be in the collection mode and select the deck yourself</i>""")
//    buildBtn.addActionListener(ActionListener(_ => exportDeck()))
//    btnPanel.add(buildBtn, BorderLayout.CENTER)
//
//


    val createBtn = new JButton("Construct")
    createBtn.setToolTipText("""<html><b>Automatically creates this deck in Hearthstone</b><br/>
						        (providing you have the required cards)<br/>
						        <br/>
						        <i>You need to be in the collection mode and select the hero yourself</i>""")
    createBtn.addActionListener(ActionListener(_ => createDeck(selectedDeck)))

    add(createBtn, BorderLayout.SOUTH)

    def selectedDeck: Option[Deck] =
      comboBox.getSelectedItem match {
        case deck: Deck => Some(deck)
        case _ => None
      }

    def selectedDeckId: Option[Int] =
      selectedDeck.map(_.id)

    def applyDecks(): Unit = {
      comboBox.removeAllItems()
      comboBox.addItem(t("deck_slot.empty"))
      val decks = DeckUtils.getDeckLists.sortBy(d => (d.hero, d.name))
      for (deck <- decks) {
        comboBox.addItem(deck)
        if (deck.activeSlot == Some(slot)) comboBox.setSelectedItem(deck)
      }
    }
  }
}
