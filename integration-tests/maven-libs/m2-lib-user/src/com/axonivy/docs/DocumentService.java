package com.axonivy.docs;

import com.docusign.esign.model.Document;

public class DocumentService {

  public static Document createDoc(String title) {
    var doc = new Document();
    doc.setDocumentId(title);
    return doc;
  }

}
