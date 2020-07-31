package com.google.launchpod.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "channel")
public class Channel {

  @JacksonXmlProperty
  private String title = "Launchpod";

  @JacksonXmlProperty
  private String link = "https://launchpod-step18-2020.appspot.com";

  @JacksonXmlProperty
  private String language;

  @JacksonXmlProperty
  private String description;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "owner", namespace = "http://www.itunes.com/dtds/podcast-1.0.dtd")
  private List<ItunesOwner> itunesOwner;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "author", namespace = "http://www.itunes.com/dtds/podcast-1.0.dtd")
  private String author;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "category", namespace = "http://www.itunes.com/dtds/podcast-1.0.dtd")
  private List<ItunesCategory> itunesCategory;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty
  private List<Item> items;

  public Channel(String name, String email, String podcastTitle, String description, String category, String language) {
    this.itunesOwner = new ArrayList<>(Arrays.asList(new ItunesOwner(name, email)));
    this.itunesCategory = new ArrayList<>(Arrays.asList(new ItunesCategory(category)));
    this.items = new ArrayList<>();
    this.author = name;
    this.language = language;
    this.description = description;
  }

  public void setLanguage(String newLanguage) {
    this.language = newLanguage;
  }

  public void setDescription(String newDescription) {
    this.description = newDescription;
  }

  public String getLanguage() {
    return this.language;
  }

  public String getDescription() {
    return this.description;
  }

  public List<Item> getItems() {
    return this.items;
  }

  /**
  * Add an item to a channel.
  */
  public static void addItem(Channel channel, String podcastTitle, String description, String language, String email, String mp3Link) {
    Item item = new Item(podcastTitle, description, language, email, mp3Link);
    channel.getItems().add(item);
  }
}
