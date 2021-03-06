package com.google.launchpod.data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import org.joda.time.DateTimeUtils;

@JacksonXmlRootElement(localName = "item")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Item {

  @JacksonXmlProperty
  @JsonProperty("title")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String title;

  @JacksonXmlProperty
  @JsonProperty("link")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String link;

  @JacksonXmlProperty
  @JsonProperty("description")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String description;

  @JacksonXmlProperty
  @JsonProperty("language")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String language;

  @JacksonXmlProperty
  @JsonProperty("email")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String email;

  @JacksonXmlProperty
  @JsonProperty("pubDate")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String pubDate;

  private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS z");

  /**
   * constructor generated for serialization/deserialization. Ensures that a
   * constructor is being read for object conversion
   */
  public Item() {
  }

  public Item(String podcastTitle, String podcastDescription, String language, String email, String mp3Link) {
    this.title = podcastTitle;
    this.description = podcastDescription;
    this.language = language;
    this.email = email;
    this.link = mp3Link;
    this.pubDate = DATE_FORMATTER.format(DateTimeUtils.currentTimeMillis());
  }

  public void setTitle(String newTitle) {
    this.title = newTitle;
  }

  public void setDescription(String newDescription) {
    this.description = newDescription;
  }

  public void setLanguage(String newLanguage){
    this.language = newLanguage;
  }

  public String getTitle() {
    return this.title;
  }

  public String getDescription() {
    return this.description;
  }

  public String getLanguage(){
    return this.language;
  }
}
