/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2000-06 The eXist Project
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 *  $Id$
 */
package org.exist.dom.test;

import java.io.File;
import java.util.Iterator;

import org.apache.log4j.BasicConfigurator;
import org.custommonkey.xmlunit.XMLTestCase;
import org.exist.collections.Collection;
import org.exist.collections.IndexInfo;
import org.exist.dom.AbstractNodeSet;
import org.exist.dom.DocumentSet;
import org.exist.dom.ExtArrayNodeSet;
import org.exist.dom.NodeProxy;
import org.exist.dom.NodeSet;
import org.exist.dom.NodeSetHelper;
import org.exist.dom.QName;
import org.exist.security.SecurityManager;
import org.exist.security.xacml.AccessContext;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.ElementValue;
import org.exist.storage.serializers.Serializer;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.util.Configuration;
import org.exist.util.XMLFilenameFilter;
import org.exist.xquery.ChildSelector;
import org.exist.xquery.DescendantOrSelfSelector;
import org.exist.xquery.DescendantSelector;
import org.exist.xquery.NameTest;
import org.exist.xquery.NodeSelector;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Test basic {@link org.exist.dom.NodeSet} operations to ensure that
 * the used algorithms are basically correct.
 *  
 * @author wolf
 *
 */
public class BasicNodeSetTest extends XMLTestCase {

	private static String directory = "samples/shakespeare";
    
    private static File dir = new File(directory);
    
	public static void main(String[] args) {
		BasicConfigurator.configure();
		junit.textui.TestRunner.run(BasicNodeSetTest.class);
	}

	private BrokerPool pool = null;
	private Collection root = null;
	
	public void testSelectors() {
		DBBroker broker = null;
        try {
        	assertNotNull(pool);
            broker = pool.get(SecurityManager.SYSTEM_USER);
            assertNotNull(broker);
            
            System.out.println("Testing ChildSelector ...");
            Sequence seq = executeQuery(broker, "//SPEECH", 2628, null);
            NameTest test = new NameTest(Type.ELEMENT, new QName("LINE", ""));
            NodeSelector selector = new ChildSelector(seq.toNodeSet(), -1);
            NodeSet set = broker.getElementIndex().findElementsByTagName(ElementValue.ELEMENT, seq.getDocumentSet(), 
            		test.getName(), selector);
            assertEquals(9492, set.getLength());
            System.out.println("ChildSelector: PASS");
            
            System.out.println("Testing DescendantOrSelfSelector ...");
            selector = new DescendantOrSelfSelector(seq.toNodeSet(), -1);
            test = new NameTest(Type.ELEMENT, new QName("SPEECH", ""));
            set = broker.getElementIndex().findElementsByTagName(ElementValue.ELEMENT, seq.getDocumentSet(), 
            		test.getName(), selector);
            assertEquals(2628, set.getLength());
            System.out.println("DescendantOrSelfSelector: PASS");
            
            System.out.println("Testing DescendantSelector ...");
            seq = executeQuery(broker, "//SCENE", 72, null);
            test = new NameTest(Type.ELEMENT, new QName("SPEAKER", ""));
            selector = new DescendantSelector(seq.toNodeSet(), -1);
            set = broker.getElementIndex().findElementsByTagName(ElementValue.ELEMENT, seq.getDocumentSet(), 
            		test.getName(), selector);
            assertEquals(2639, set.getLength());
            System.out.println("DescendantSelector: PASS");
            
        } catch (Exception e) {
	        fail(e.getMessage());
        } finally {
        	if (pool != null) pool.release(broker);
        }
	}
	
	public void testAxes() {
		DBBroker broker = null;
        try {
        	assertNotNull(pool);
            broker = pool.get(SecurityManager.SYSTEM_USER);
            assertNotNull(broker);
            
            Serializer serializer = broker.getSerializer();
            serializer.reset();
            DocumentSet docs = root.allDocs(broker, new DocumentSet(), true, false);
            
            Sequence smallSet = executeQuery(broker, "//SPEECH[LINE &= 'perturbed spirit']", 1, null);
            Sequence largeSet = executeQuery(broker, "//SPEECH[LINE &= 'love']", 160, null);
            Sequence outerSet = executeQuery(broker, "//SCENE[TITLE &= 'closet']", 1, null);
            
            NameTest test = new NameTest(Type.ELEMENT, new QName("SPEAKER", ""));
            NodeSet speakers = broker.getElementIndex().findElementsByTagName(ElementValue.ELEMENT,
                    docs, test.getName(), null);
            
            System.out.println("Testing NodeSetHelper.selectParentChild ...");
            NodeSet result = NodeSetHelper.selectParentChild(speakers, smallSet.toNodeSet(), NodeSet.DESCENDANT, -1);
            assertEquals(1, result.getLength());
            String value = serialize(broker, result.itemAt(0));
            System.out.println("NodeSetHelper.selectParentChild: " + value);
            assertEquals(value, "<SPEAKER>HAMLET</SPEAKER>");
            
            result = NodeSetHelper.selectParentChild(speakers, largeSet.toNodeSet(), NodeSet.DESCENDANT, -1);
            assertEquals(160, result.getLength());
            System.out.println("NodeSetHelper.selectParentChild: PASS");
            
            System.out.println("Testing AbstractNodeSet.quickSelectParentChild ...");
            result = ((AbstractNodeSet)speakers).quickSelectParentChild(smallSet.toNodeSet(), NodeSet.DESCENDANT, -1);
            assertEquals(1, result.getLength());
            value = serialize(broker, result.itemAt(0));
            System.out.println("AbstractNodeSet.quickSelectParentChild: " + value);
            assertEquals(value, "<SPEAKER>HAMLET</SPEAKER>");
            
            result = ((AbstractNodeSet)speakers).quickSelectParentChild(largeSet.toNodeSet(), NodeSet.DESCENDANT, -1);
            assertEquals(160, result.getLength());
            System.out.println("AbstractNodeSet.quickSelectParentChild: PASS");
            
            System.out.println("Testing AbstractNodeSet.hasChildrenInSet ...");
            result = ((AbstractNodeSet)speakers).hasChildrenInSet(smallSet.toNodeSet(), NodeSet.DESCENDANT, -1);
            assertEquals(1, result.getLength());
            value = serialize(broker, result.itemAt(0));
            System.out.println("AbstractNodeSet.hasChildrenInSet: " + value);
            assertEquals(value, "<SPEAKER>HAMLET</SPEAKER>");
            
            result = ((AbstractNodeSet)speakers).hasChildrenInSet(largeSet.toNodeSet(), NodeSet.DESCENDANT, -1);
            assertEquals(160, result.getLength());
            System.out.println("AbstractNodeSet.hasChildrenInSet: PASS");
            
            System.out.println("Testing AbstractNodeSet.selectAncestorDescendant ...");
            result = speakers.selectAncestorDescendant(outerSet.toNodeSet(), NodeSet.DESCENDANT, false, -1);
            assertEquals(56, result.getLength());
            System.out.println("AbstractNodeSet.selectAncestorDescendant: PASS");
            
            System.out.println("Testing AbstractNodeSet.selectAncestorDescendant2 ...");
            result = ((AbstractNodeSet)outerSet).selectAncestorDescendant(outerSet.toNodeSet(), NodeSet.DESCENDANT, true, -1);
            assertEquals(1, result.getLength());
            System.out.println("AbstractNodeSet.selectAncestorDescendant2: PASS");
        } catch (Exception e) {
        	e.printStackTrace();
	        fail(e.getMessage());
        } finally {
        	if (pool != null) pool.release(broker);
        }
	}
	
	private Sequence executeQuery(DBBroker broker, String query, int expected,
			String expectedResult) throws XPathException, SAXException {
		XQuery xquery = broker.getXQueryService();
		assertNotNull(xquery);
		Sequence seq = xquery.execute(query, null, AccessContext.TEST);
		assertNotNull(seq);
		assertEquals(expected, seq.getLength());
		System.out.println("Found: " + seq.getLength() + " for query:\n" + query);
		if (expectedResult != null) {
	        Item item = seq.itemAt(0);
	        String value = serialize(broker, item);
	        assertEquals(expectedResult, value);
		}
		return seq;
	}

	private String serialize(DBBroker broker, Item item) throws SAXException, XPathException {
		Serializer serializer = broker.getSerializer();
		assertNotNull(serializer);
        serializer.reset();
		String value;
		if (Type.subTypeOf(item.getType(), Type.NODE))
			value = serializer.serialize((NodeValue) item);
		else
			value = item.getStringValue();
		return value;
	}
	
	protected void setUp() throws Exception {        
        DBBroker broker = null;
        try {
        	pool = startDB();
        	assertNotNull(pool);
            broker = pool.get(SecurityManager.SYSTEM_USER);
            assertNotNull(broker);            
            TransactionManager transact = pool.getTransactionManager();
            assertNotNull(transact);
            Txn transaction = transact.beginTransaction();
            assertNotNull(transaction);            
            System.out.println("BasicNodeSetTest#setUp ...");
            
            root = broker.getOrCreateCollection(transaction, DBBroker.ROOT_COLLECTION + "/test");
            assertNotNull(root);
            broker.saveCollection(transaction, root);
            
            
            File files[] = dir.listFiles(new XMLFilenameFilter());
            assertNotNull(files);
            
            File f;
            IndexInfo info;
            // store some documents.
            for (int i = 0; i < files.length; i++) {
                f = files[i];
                try {
                    info = root.validateXMLResource(transaction, broker, f.getName(), new InputSource(f.toURI().toASCIIString()));
                    assertNotNull(info);
                    root.store(transaction, broker, info, new InputSource(f.toURI().toASCIIString()), false);
                } catch (SAXException e) {
                    System.err.println("Error found while parsing document: " + f.getName() + ": " + e.getMessage());
                }
            }
            
            transact.commit(transaction);
            System.out.println("BasicNodeSetTest#setUp finished.");
        } catch (Exception e) {            
	        fail(e.getMessage()); 	        
        } finally {
        	if (pool != null) pool.release(broker);
        }
	}
	
	protected BrokerPool startDB() {
        String home, file = "conf.xml";
        home = System.getProperty("exist.home");
        if (home == null)
            home = System.getProperty("user.dir");
        try {
            Configuration config = new Configuration(file, home);
            BrokerPool.configure(1, 5, config);
            return BrokerPool.getInstance();
        } catch (Exception e) {            
            fail(e.getMessage());
        }
        return null;
    }

    protected void tearDown() {
        BrokerPool.stopAll(false);
    }
}
