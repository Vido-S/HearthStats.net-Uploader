/**
 * @author shyos
 */
package zulu.deckexport.extracter;

import zulu.deckexport.card.DeckItemImage;

import java.awt.image.BufferedImage;

public class CropManager {

	public final BufferedImage subImage;		// Text Image of a DeckItem
	public final BufferedImage countImage;		// Count Image of a DeckItem
	public final BufferedImage deckItemImage;


  public CropManager(int order, BufferedImage image) {
    DeckItemImage card = new DeckItemImage(order);

    subImage = image.getSubimage(card.getText().getX(),
      card.getText().getY(),
      card.getText().getW(),
      card.getText().getH()); //crop image

    countImage = image.getSubimage(card.getCount().getX(),
      card.getCount().getY(),
      card.getCount().getW(),
      card.getCount().getH());

    deckItemImage = image.getSubimage(card.getFull().getX(),
      card.getFull().getY(),
      card.getFull().getW(),
      card.getFull().getH());

  }
}
