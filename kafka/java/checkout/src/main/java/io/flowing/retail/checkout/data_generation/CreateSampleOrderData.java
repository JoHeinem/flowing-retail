/*
 * Copyright © 2012 - 2018 camunda services GmbH and various authors (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.flowing.retail.checkout.data_generation;

import io.flowing.retail.checkout.domain.Customer;
import io.flowing.retail.checkout.domain.Order;
import io.flowing.retail.checkout.messages.Message;
import io.flowing.retail.checkout.messages.MessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Component
public class CreateSampleOrderData implements CommandLineRunner {

  private static final Customer[] customers = new Customer[]{
    new Customer("Camunda", "Zossener Strasse 55\n10961 Berlin\nGermany"),
    new Customer("Felix Mueller", "Product manager square 15\n11111 Produkthausen\nGermany"),
    new Customer("Jakob Freund", "CEO Street 1\n123456 Berlin\nGermany"),
    new Customer("Johannes Heinemann", "Boss Avenue 55\n333444 Bosshausen\nGermany"),
    new Customer("Sebastian Stamm", "Developers slayer cave 100\n83103 \nGermany"),
    new Customer("Sebastian Bathke", "Sacred technical knowledge grove  77\n95031 Heiligenstadt\nGermany"),
    new Customer("Michael Wagner", "Tower of strength lane\n14547 Busendorf\nGermany"),
    new Customer("Omran Abazeed", "Zossener Strasse 55\n10961 Berlin\nGermany"),
    new Customer("Kyrylo Zakurdaiev", "Zossener Strasse 55\n10961 Berlin\nGermany"),
    new Customer("Luke Opti", "Process owner road 19\n15363 Munich\nGermany"),
  };

  private static final String[] items = {
    "Demonic Crown",
    "Demon Urn",
    "Anarchy Skull",
    "Impurity Tablet",
    "Transformation Grimoire",
    "Feather of Resistance",
    "Crown of Fire",
    "Instrument of Acrimony",
    "Skull of the Occult",
    "Book of Lightness",
    "Philosopher's Seal",
    "Spite Amulet",
    "Isolation Scroll",
    "Torment Statuette",
    "Titan Chalice",
    "Cup of Spells",
    "Scroll of Infinity",
    "Fruit of Time",
    "Statuette of Pestilence",
    "Circlet of Massacres",
    "Curse Stone",
    "Gold Seal",
    "Blinding Tablet",
    "Finality Ark",
    "Lifeblood Band",
    "Statuette of Rain",
    "Tome of Mending",
    "Book of Spells",
    "Canopic Jar of Ruination",
    "Amulet of Betrayal",
    "Fate's Root",
    "Prime Hand",
    "Alchemy Cloak",
    "Storm Disc",
    "Paralyzing Necklace",
    "Key of Acrimony",
    "Letters of Dread",
    "Gauntlet of Spells",
    "Ark of Collapse",
    "Scroll of Resistance",
    "Warding Hide",
    "Finality Band",
    "Vengeance Ring",
    "Delusion Texts",
    "Ice Statue",
    "Arch of Faith",
    "Ark of Vengeance",
    "Microlith of Liberation",
    "Chest of Toxin",
    "Fleece of the Void",
    "Celestial Key",
    "Infinity Amulet",
    "Eradication Boots",
    "Guardian's Robes",
    "Chaos Tiara",
    "Statue of Spells",
    "Ichor of Heroism",
    "Rod of Passion",
    "Chalice of Misfortune",
    "Sandals of Specters",
    "Maniacal Fleece",
    "Destiny's Tiara",
    "Karma Tablet",
    "Exiled Rod",
    "Massacre Fleece",
    "Gem of Sleep",
    "Cylinder of the Cosmos",
    "Chest of Fire",
    "Necklace of Evil",
    "Amulet of Immobilizing",
    "Torture Sandals",
    "Paragon Chest",
    "Scourge Boots",
    "Doom Canopic Chest",
    "Transmutation Sword",
    "Letters of Exiles",
    "Tome of Frost",
    "Canopic Jar of Apathy",
    "Slab of Loyalty",
    "Stone of Karma",
    "Prosperous Key",
    "Slumber Arch",
    "Supremacy Book",
    "Spellbound Skull",
    "Blight Skull",
    "Staff of Lust",
    "Cloak of Sanctification",
    "Jar of Betrayal",
    "Ring of Riddles",
    "Cloak of Blessings",
    "Rebirth Sandals",
    "Heavenly Urn",
    "Wisdom Rod",
    "Spite Runes",
    "Warding Key",
    "Inscriptions of Venom",
    "Skull of Paralysis",
    "Cube of Revival",
    "Runes of Genesis",
    "Chalice of Youth",
    "Rebirth Sandals",
    "Heavenly Urn",
    "Wisdom Rod",
    "Spite Runes",
    "Warding Key",
    "Inscriptions of Venom",
    "Skull of Paralysis",
    "Cube of Revival",
    "Runes of Genesis",
    "Chalice of Youth"
  };
  @Autowired
  private MessageSender messageSender;
  @Value("${org.camunda.optimize.order-count}")
  private int orderCount;

  @Override
  public void run(String... args) throws Exception {

    IntStream.range(0, orderCount).forEach(ignored -> {
      int countOfItemsToAdd = ThreadLocalRandom.current().nextInt(2, 8);
      Order order = new Order();
      IntStream.range(1, countOfItemsToAdd).forEach(
        foo -> {
          String randomItem = items[ThreadLocalRandom.current().nextInt(0, items.length)];
          int itemAmount = ThreadLocalRandom.current().nextInt(1, 6);
          order.addItem(
            randomItem,
            itemAmount
          );
        }
      );
      Customer customer = customers[ThreadLocalRandom.current().nextInt(0, customers.length)];
      order.setCustomer(customer);

      Message<Order> message = new Message<Order>("OrderPlacedEvent", order);
      messageSender.send(message);
    });
  }
}
