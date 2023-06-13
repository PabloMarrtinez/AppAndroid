package inf.um.pilotomimurcia.eidas;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import inf.um.pilotomimurcia.SignUp;
import inf.um.pilotomimurcia.EIDASActivity;

public class MyJavaScriptInterface {

    private static final String TAG = MyJavaScriptInterface.class.getSimpleName();
    private EIDASActivity ctx;

    public MyJavaScriptInterface(EIDASActivity ctx) {
        this.ctx = ctx;
    }

    @SuppressWarnings("unused")
    @JavascriptInterface
    public void showHTML(String html) {
        Log.d(TAG, "showHTML: HTML -> " + html);
        if (!html.contains("saml2p:"))
            return;
        Document xmlDocument = convertStringToDocument(html);

        assert xmlDocument != null;
        Log.d(TAG, "showHTML: size -> " + xmlDocument.getElementsByTagName("umukeyrockresponse").getLength());
        Node saml2Response = xmlDocument.getElementsByTagName("umukeyrockresponse").item(0).getLastChild();
        if (saml2Response != null) {
            String textContent = saml2Response.getTextContent();
            Log.d(TAG, "showHTML: p hidden ->" + saml2Response.getTextContent());
            LOGLongString(saml2Response.getTextContent());
            ctx.finish(saml2Response.getTextContent());

        }
    }

    private void LOGLongString(String veryLongString) {
        Log.d(TAG, "LOGLongString: start");
        int maxLogSize = 1000;
        for (int i = 0; i <= veryLongString.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i + 1) * maxLogSize;
            end = end > veryLongString.length() ? veryLongString.length() : end;
            Log.v(TAG, veryLongString.substring(start, end));
        }
        Log.d(TAG, "LOGLongString: end");
    }

    private static Document convertStringToDocument(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
