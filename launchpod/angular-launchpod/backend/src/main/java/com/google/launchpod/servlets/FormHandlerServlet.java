package com.google.launchpod.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.auth.appengine.AppEngineCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.launchpod.data.RSS;
import com.google.launchpod.data.UserFeed;
import com.google.launchpod.data.MP3;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.PostPolicyV4;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

@WebServlet("/rss-feed")
@MultipartConfig
public class FormHandlerServlet extends HttpServlet {

  public static final String PROJECT_ID = "launchpod-step18-2020"; // The ID of your GCP project
  public static final String BUCKET_NAME = "launchpod-mp3-files"; // The ID of the GCS bucket to upload to

  public static final String USER_FEED = "UserFeed";
  public static final String PODCAST_TITLE = "title";
  public static final String DESCRIPTION = "description";
  public static final String LANGUAGE = "language";
  public static final String EMAIL = "email";
  public static final String TIMESTAMP = "timestamp";
  public static final String MP3 = "mp3";
  public static final String MP3_LINK = "mp3Link";
  public static final String XML_STRING = "xmlString";
  public static final String PUB_DATE = "pubDate";
  public static final String GENERATE_RSS_URL = "https://launchpod-step18-2020.appspot.com/rss-feed?action=generateRSSLink&id=";
  public static final String HTML_FORM_END = "  <input type='file' name='file'/><br />\n"
                                             + "<input type='submit' value='Upload File' name='submit'/><br />\n"
                                             + "</form>\n";

  private static final String ID = "id";
  private static final String ACTION = "action";

  private enum Action {
    generateRSSLink("generateRSSLink"),
    generateXml("generateXml"),
    otherAction("otherAction"); // for testing purposes

    private String action;
 
    Action(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
      return action;
    }
  }

  /**
   * Requests user inputs in form fields, then creates Entity and places in Datastore.
   * @throws ServletException
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws IllegalArgumentException, IOException {
    String podcastTitle = req.getParameter(PODCAST_TITLE);
    String description = req.getParameter(DESCRIPTION);
    String language = req.getParameter(LANGUAGE);
    String email = req.getParameter(EMAIL);

    // TO-DO after merging: add this in and remove email parameter (because LoginServlet will be set up)
    // UserService userService = UserServiceFactory.getUserService();
    // if (userService.isUserLoggedIn()) {
    //   email = userService.getCurrentUser().getEmail();
    // }

    if (podcastTitle == null || podcastTitle.isEmpty()) {
      throw new IllegalArgumentException("No Title inputted, please try again.");
    } else if (description == null || description.isEmpty()) {
      throw new IllegalArgumentException("No description inputted, please try again.");
    } else if (language == null || language.isEmpty()) {
      throw new IllegalArgumentException("No language inputted, please try again.");
    } else if (email == null || email.isEmpty()) {
      throw new IllegalArgumentException("You are not logged in. Please try again.");
    }

    // Create entity with all desired attributes
    Entity userFeedEntity = new Entity(USER_FEED);
    userFeedEntity.setProperty(PODCAST_TITLE, podcastTitle);
    userFeedEntity.setProperty(DESCRIPTION, description);
    userFeedEntity.setProperty(LANGUAGE, language);
    userFeedEntity.setProperty(EMAIL, email);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key entityKey = datastore.put(userFeedEntity);

    String entityId = KeyFactory.keyToString(entityKey);
    String mp3Link = "https://storage.googleapis.com/" + BUCKET_NAME + "/" + entityId;

    // Create embedded entity to store MP3 data
    EmbeddedEntity mp3 = new EmbeddedEntity();
    mp3.setProperty(ID, entityId);
    mp3.setProperty(MP3_LINK, mp3Link);
    mp3.setProperty(EMAIL, email);

    // Retrieve entity from Datastore
    Entity savedEntity;
    try {
      savedEntity = datastore.get(entityKey);
    } catch(EntityNotFoundException e) {
      throw new IOException("Entity not found in Datastore.");
    }

    // Generate xml string
    RSS rssFeed = new RSS(podcastTitle, description, language, email, mp3Link);
    String xmlString = "";
    try {
      xmlString = RSS.toXmlString(rssFeed);
      savedEntity.setProperty(XML_STRING ,xmlString);
    } catch(IOException e){
      throw new IOException("Unable to create XML string.");
    }

    // Update entity by adding embedded mp3 entity as a property
    savedEntity.setProperty(MP3, mp3);
    datastore.put(savedEntity);

    //write the file upload form
    String formHtml = generateSignedPostPolicyV4(PROJECT_ID, BUCKET_NAME, entityId);
    res.setContentType("text/html");
    res.getWriter().println(formHtml);
  }

  /**
   * Generate policy for directly uploading a file to Cloud Storage via HTML form.
   * @return HTML form (as a String) for uploading MP3
   */
  public String generateSignedPostPolicyV4 (String projectId, String bucketName, String blobName) throws IOException {
    // String blobName = entity ID from Datastore, as the name of object uploaded to GCS

    String errorMessage = "Policy is null.";
    Storage storage;
    PostPolicyV4.PostFieldsV4 fields;
    PostPolicyV4 policy = null;
    String myRedirectUrl = "";

    try {
      // Prepare credentials and API key
      String keyFileName = "launchpod-step18-2020-47434aafba88.json";
      ClassLoader classLoader = getClass().getClassLoader();
      File file = new File(classLoader.getResource(keyFileName).getFile());
      GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(file));

      storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();

      myRedirectUrl = GENERATE_RSS_URL + blobName;
      fields =
          PostPolicyV4.PostFieldsV4.newBuilder().setSuccessActionRedirect(myRedirectUrl).build();

      policy =
        storage.generateSignedPostPolicyV4(
            BlobInfo.newBuilder(bucketName, blobName).build(), 10, TimeUnit.MINUTES, fields);
    } catch(Exception e) {
      errorMessage = e.getMessage();
      throw e;
    }

    // StringBuilder htmlForm;
    StringBuilder htmlForm = new StringBuilder();
    if (policy!=null) {
      // htmlForm =
      //     new StringBuilder(
      //         "<form name='mp3-upload' action='"
      //             + policy.getUrl()
      //             + "' method='POST' enctype='multipart/form-data'>\n");
      String htmlFormPt1 = String.format("<form name='mp3-upload' action='%s' method='POST' enctype='multipart/form-data'>\n", policy.getUrl());
      htmlForm.append(htmlFormPt1);

      for (Map.Entry<String, String> entry : policy.getFields().entrySet()) {
        String htmlFormPt2 = String.format("<input name='%s' value='%s' type='hidden' />\n", entry.getKey(), entry.getValue());
        htmlForm.append(htmlFormPt2);
      }
      htmlForm.append(HTML_FORM_END);
    } else {
      htmlForm = new StringBuilder();
      htmlForm.append(errorMessage);
    }
    return htmlForm.toString();
  }

  /**
  * Display RSS feed xml string that user tries recalling with the given ID.
  */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String actionString = req.getParameter(ACTION);
    String id = req.getParameter(ID);

    if (actionString==null || id==null) {
      res.setContentType("text/html");
      res.getWriter().println("Please specify action and/or id.");
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    Action act = Action.generateRSSLink; // initialization
    Action action = act.valueOf(actionString);
    
    if (action == Action.generateRSSLink) {
      // Generate link to the RSS feed
      String rssLink = "https://launchpod-step18-2020.appspot.com/rss-feed?action=generateXml&id=" + id;
      res.setContentType("text/html");
      res.getWriter().println(rssLink);
   } else if (action == Action.generateXml) {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Key entityKey = null;
      Entity desiredFeedEntity = null; 
      try {
        // Use key to retrieve entity from Datastore
        entityKey = KeyFactory.stringToKey(id);
        desiredFeedEntity = datastore.get(entityKey);

      } catch (IllegalArgumentException e) {
        // If entityId cannot be converted into a key
        e.printStackTrace();
        res.setContentType("text/html");
        res.getWriter().println("Sorry, this is not a valid id.");
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
        
      } catch (EntityNotFoundException e) {
        // No matching entity in Datastore
        e.printStackTrace();
        res.setContentType("text/html");
        res.getWriter().println("Your entity could not be found.");
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      String podcastTitle = desiredFeedEntity.getProperty(PODCAST_TITLE).toString();
      String description = desiredFeedEntity.getProperty(DESCRIPTION).toString();
      String language = desiredFeedEntity.getProperty(LANGUAGE).toString();
      String email = desiredFeedEntity.getProperty(EMAIL).toString();

      EmbeddedEntity mp3Entity = (EmbeddedEntity) desiredFeedEntity.getProperty(MP3);
      String mp3Link = mp3Entity.getProperty(MP3_LINK).toString();

      RSS rssFeed = new RSS(podcastTitle, description, language, email, mp3Link);

      XmlMapper xmlMapper = new XmlMapper();
      String xmlString = xmlMapper.writeValueAsString(rssFeed);
      res.setContentType("text/xml");
      res.getWriter().println(xmlString);
   } else {
      res.setContentType("text/html");
      res.getWriter().println("Sorry, this is not a valid action.");
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
   }
  }

  /**
   * Create RSS XML string from given fields.
   * @return xml String
   * @throws IOException
   * @throws Exception
   */
  private static String xmlString(Entity userFeedEntity) throws IOException {
    XmlMapper xmlMapper = new XmlMapper();
    String xmlString = xmlMapper.writeValueAsString(UserFeed.fromEntity(userFeedEntity));
    return xmlString;
  }
}
