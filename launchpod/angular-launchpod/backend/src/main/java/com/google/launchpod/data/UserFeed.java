package com.google.launchpod.data;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.launchpod.servlets.FormHandlerServlet;

public final class UserFeed {

  private String xmlString;
  private String title;
  private String name;
  private String rssLink;
  private String description;
  private String email;
  private String postTime;
  private long timestamp;
  private Key key;

  private UserFeed(String xmlString) {
    this.xmlString = xmlString;
  }

  public UserFeed(String title, String name, String rssLink, String description, String email, String postTime, long timestamp, Key key) {
    this.title = title;
    this.name = name;
    this.rssLink = rssLink;
    this.description = description;
    this.email = email;
    this.postTime = postTime;
    this.timestamp = timestamp;
    this.key = key;
  }

  /**
   * Create UserFeed Object from Entity
   * 
   * @param entity : entity that is being used to create user feed object
   * @return UserFeed object
   */
  public static UserFeed fromEntity(Entity entity) {
    String xmlString = (String) entity.getProperty(FormHandlerServlet.XML_STRING);
    return new UserFeed(xmlString);
  }

  // getter for xml string
  public String getXmlString() {
    return this.xmlString;
  }
}
