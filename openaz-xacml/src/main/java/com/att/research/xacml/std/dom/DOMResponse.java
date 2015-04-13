/*
 *                        AT&T - PROPRIETARY
 *          THIS FILE CONTAINS PROPRIETARY INFORMATION OF
 *        AT&T AND IS NOT TO BE DISCLOSED OR USED EXCEPT IN
 *             ACCORDANCE WITH APPLICABLE AGREEMENTS.
 *
 *          Copyright (c) 2013 AT&T Knowledge Ventures
 *              Unpublished and Not for Publication
 *                     All Rights Reserved
 */
package com.att.research.xacml.std.dom;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;

import javax.security.auth.x500.X500Principal;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.att.research.xacml.api.Advice;
import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeAssignment;
import com.att.research.xacml.api.AttributeCategory;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.IdReference;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.MissingAttributeDetail;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import com.att.research.xacml.api.SemanticString;
import com.att.research.xacml.api.Status;
import com.att.research.xacml.api.StatusCode;
import com.att.research.xacml.api.StatusDetail;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.StdMutableResponse;
import com.att.research.xacml.std.StdResponse;
import com.att.research.xacml.std.StdStatusCode;
import com.att.research.xacml.std.datatypes.ExtendedNamespaceContext;
import com.att.research.xacml.std.datatypes.XPathExpressionWrapper;
import com.att.research.xacml.std.json.JSONStructureException;

/**
 * DOMResponse is used to convert XML into {@link com.att.research.xacml.api.Response} objects and 
 * {@link com.att.research.xacml.api.Response} objects into XML strings.
 * Instances of this class are never created.
 * The {@link com.att.research.xacml.api.Response} objects returned by this class are instances of
 * {@link com.att.research.xacml.std.StdMutableResponse}.
 * {@link com.att.research.xacml.api.Response} objects are generated by loading a file or XML Node tree representing the Request.
 * In normal product operation this is not used to generate new instances
 * because the PDP generates {@link com.att.research.xacml.std.StdResponse} objects internally.
 * Those objects are converted to XML strings for transmission through the RESTful Web Service
 * using the <code>convert</code> method in this class.
 * 
 * @author car
 * @version $Revision: 1.2 $
 */
public class DOMResponse {
	private static final Log logger	= LogFactory.getLog(DOMResponse.class);
	
	protected DOMResponse() {
	}
	
	
	
	/**
	 * Creates a new <code>DOMResponse</code> by parsing the given <code>Node</code> representing a XACML Response element.
	 * 
	 * @param nodeResponse the <code>Node</code> representing the XACML Response element
	 * @return a new <code>DOMResponse</code> parsed from the given <code>Node</code>
	 * @throws DOMStructureException if the conversion cannot be made
	 */
	public static Response newInstance(Node nodeResponse) throws DOMStructureException {
		Element	elementResponse		= DOMUtil.getElement(nodeResponse);
		boolean bLenient			= DOMProperties.isLenient();
		
		StdMutableResponse mutableResponse		= new StdMutableResponse();
		
		NodeList children			= elementResponse.getChildNodes();
		int numChildren;
		boolean sawResult			= false;
		if (children != null && (numChildren = children.getLength()) > 0) {
			for (int i = 0 ; i < numChildren ; i++) {
				Node child	= children.item(i);
				if (DOMUtil.isElement(child)) {
					if (DOMUtil.isInNamespace(child, XACML3.XMLNS)&& XACML3.ELEMENT_RESULT.equals(child.getLocalName())) {
						mutableResponse.add(DOMResult.newInstance(child));
						sawResult	= true;
					} else {
						if (!bLenient) {
							throw DOMUtil.newUnexpectedElementException(child, nodeResponse);
						}
					}
				}
			}
		}
		if (!sawResult && !bLenient) {
			throw DOMUtil.newMissingElementException(nodeResponse, XACML3.XMLNS, XACML3.ELEMENT_RESULT);
		}
		
		return new StdResponse(mutableResponse);
	}
	
	/**
	 * Change XACML2 into XACML3
	 * 
	 * @param nodeResponse
	 * @return
	 * @throws DOMStructureException
	 */
	public static boolean repair(Node nodeResponse) throws DOMStructureException {
		Element	elementResponse		= DOMUtil.getElement(nodeResponse);
		boolean result				= false;
		
		NodeList children			= elementResponse.getChildNodes();
		int numChildren;
		boolean sawResult			= false;
		if (children != null && (numChildren = children.getLength()) > 0) {
			for (int i = 0 ; i < numChildren ; i++) {
				Node child	= children.item(i);
				if (DOMUtil.isElement(child)) {
					if (DOMUtil.isInNamespace(child, XACML3.XMLNS)&& XACML3.ELEMENT_RESULT.equals(child.getLocalName())) {
						result	= DOMResult.repair(child) || result;
						sawResult	= true;
					} else {
						logger.warn("Unexpected element " + child.getNodeName());
						elementResponse.removeChild(child);
						result	= true;
					}
				}
			}
		}
		
		if (!sawResult) {
			throw DOMUtil.newMissingElementException(nodeResponse, XACML3.XMLNS, XACML3.ELEMENT_RESULT);
		}
		return result;
	}

	

	public static Response load(String xmlString) throws DOMStructureException {
		Response response = null;
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
			response = load(is);
		} catch (Exception ex) {
			throw new DOMStructureException("Exception loading String Response: " + ex.getMessage(), ex);
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch(Exception idontcare) {				
			}
		}
		return response;
	}
	
	
	/**
	 * Read a file containing an XML representation of a Response and parse it into a {@link com.att.research.xacml.api.Response} Object.
	 * This is used only for testing since Responses in the normal environment are generated by the PDP code.
	 * 
	 * @param fileResponse
	 * @return
	 * @throws DOMStructureException
	 */
	public static Response load(File fileResponse) throws DOMStructureException {
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileResponse));
			String responseString = "";
			String line;
			while ((line = br.readLine()) != null) {
				responseString += line;
			}
			br.close();
			return load(responseString);
		} catch (Exception e) {
			throw new DOMStructureException("File: " + fileResponse.getName() + " " + e.getMessage());
		}
	}
	
	/**
	 * Loads a response from java nio Path object.
	 * 
	 * @param fileResponse
	 * @return
	 * @throws DOMStructureException
	 */
	public static Response load(Path fileResponse) throws DOMStructureException {
		try {
			return DOMResponse.load(Files.newInputStream(fileResponse));
		} catch (Exception e) {
			throw new DOMStructureException(e);
		}
	}
	
	
	/**
	 * Read a file containing an XML representation of a Response and parse it into a {@link com.att.research.xacml.api.Response} Object.
	 * This is used only for testing since Responses in the normal environment are generated by the PDP code.
	 * 
	 * @param fileResponse
	 * @return
	 * @throws DOMStructureException
	 */
	public static Response load(InputStream is) throws DOMStructureException {
		/*
		 * Get the DocumentBuilderFactory
		 */
		DocumentBuilderFactory documentBuilderFactory	= DocumentBuilderFactory.newInstance();
		if (documentBuilderFactory == null) {
			throw new DOMStructureException("No XML DocumentBuilderFactory configured");
		}
		documentBuilderFactory.setNamespaceAware(true);
		
		/*
		 * Get the DocumentBuilder
		 */
		DocumentBuilder documentBuilder	= null;
		try {
			documentBuilder	= documentBuilderFactory.newDocumentBuilder();
		} catch (Exception ex) {
			throw new DOMStructureException("Exception creating DocumentBuilder: " + ex.getMessage(), ex);
		}
		
		/*
		 * Parse the XML file
		 */
		Document document	= null;
		Response request	= null;
		try {
			document	= documentBuilder.parse(is);
			if (document == null) {
				throw new Exception("Null document returned");
			}
			
			Node rootNode	= document.getFirstChild();
			if (rootNode == null) {
				throw new Exception("No child in document");
			}
			
			if (DOMUtil.isInNamespace(rootNode, XACML3.XMLNS)) {
				if (XACML3.ELEMENT_RESPONSE.equals(rootNode.getLocalName())) {
					request	= DOMResponse.newInstance(rootNode);
					if (request == null) {
						throw new DOMStructureException("Failed to parse Response");
					}
				} else {
					throw DOMUtil.newUnexpectedElementException(rootNode);
				}
			} else {
				throw DOMUtil.newUnexpectedElementException(rootNode);
			}
		} catch (Exception ex) {
			throw new DOMStructureException("Exception loading Response from InputStream: " + ex.getMessage(), ex);
		}
		return request;
	}
	
	
	/**
	 * Convert the {@link com.att.research.xacml.api.Response} into an XML string.
	 * This is used only for debugging.
	 * It assumes that pretty-printing is desired.
	 * 
	 * @param response
	 * @return
	 * @throws Exception 
	 */
	public static String toString(Response response) throws Exception {
		return toString(response, true);
	}
	
	/**
	 * Convert the {@link com.att.research.xacml.api.Response} into an XML string.
	 * This is used only for debugging.
	 * The caller chooses whether to pretty-print or not.
	 * 
	 * @param response
	 * @param prettyPrint
	 * @return
	 * @throws Exception 
	 */
	public static String toString(Response response, boolean prettyPrint) throws Exception {
		String outputString = null;
		ByteArrayOutputStream os = null;
		try {
			os = new ByteArrayOutputStream();
			DOMResponse.convert(response, os, prettyPrint);
			outputString = new String( os.toByteArray(), "UTF-8");
		} catch (Exception ex) {
			throw ex;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
			} catch(Exception idontcare) {				
			}
		}
		return outputString;
	}
	

	/**
	 * Helper - recursively output StatusCode objects as XML.
	 * 
	 * @param sb
	 * @param statusCode
	 * @param tabCount
	 * @param prettyPrint
	 */
	private static void outputStatusCode(StringBuffer sb, StatusCode statusCode, int tabCount, boolean prettyPrint) {
		String prettyPrintString = "";
		if (prettyPrint) {
			prettyPrintString = "\n";
			for (int i = 0; i < tabCount; i++) {
				prettyPrintString += "\t";
			}
		}
		
		sb.append(prettyPrintString);
		sb.append("<StatusCode");
		
		if (statusCode.getStatusCodeValue() != null) {
			sb.append(" Value=\"" + statusCode.getStatusCodeValue().stringValue() + "\"");
		} 
		
		if (statusCode.getChild() == null) {
			// no child code, so finish off the StatusCode element now
			sb.append("/>");
		} else {
			// there is a child, so need to use the two-part notation for this StatusCode
			sb.append(">");
			outputStatusCode(sb, statusCode.getChild(), tabCount + 1, prettyPrint);
			sb.append(prettyPrintString);
			sb.append("</StatusCode>");
		}
	}
	
	/**
	 * Helper: When outputting as XML string, get the value of a Value (within an AttributeValue) object as a String.
	 * Most of these objects are SemanticStrings, but some are not and we cannot assume that in the places where we need to generate the output.
	 * 
	 * @param obj
	 * @return String 
	 * @throws JSONStructureException
	 * @throws DOMStructureException 
	 */
	private static String outputValueValue(Object obj) throws DOMStructureException {
		if (obj instanceof String ||
				obj instanceof Boolean ||
				obj instanceof Integer ||
				obj instanceof BigInteger ) {
			return obj.toString();
		} else if (obj instanceof Double) {
			Double d = (Double)obj;
			if (d == Double.NaN) {
				return "NaN";
			} else if (d == Double.POSITIVE_INFINITY) {
				return "INF";
			} else if (d == Double.NEGATIVE_INFINITY) {
				return "-INF";
			}
			return obj.toString();
		} else if (obj instanceof SemanticString) {
			return ((SemanticString)obj).stringValue();
		} else if (obj instanceof X500Principal ||
				obj instanceof URI) {
			// something is very weird with X500Principal data type.  If left on its own the output is a map that includes encoding.
			return obj.toString();
		} else if (obj instanceof XPathExpressionWrapper) {

			XPathExpressionWrapper xw = (XPathExpressionWrapper) obj;

			return xw.getPath();
		} else {
			throw new DOMStructureException("Unhandled data type='" + obj.getClass().getName() + "'");
		}
	}
	
	
	/**
	 * Helper: When outputting as XML string, extract any Namespace info from an AttributeValue.Value.Value object.
	 * This must be done separately from getting the Value as a String because this info is put as an attribute in the surrounding element.
	 * Currently only applies to XPathExpressionWrappers.
	 * 
	 * @param valueObject
	 * @return
	 */
	private static String getNamespaces(Object valueObject) {
		String returnString = "";
		if ( ! (valueObject instanceof XPathExpressionWrapper)) {
			// value is not XPathExpression, so has no Namespace info in it
			return returnString;
		}
		XPathExpressionWrapper xw = (XPathExpressionWrapper) valueObject;
		
		ExtendedNamespaceContext namespaceContext = xw.getNamespaceContext();
		if (namespaceContext != null) {
			// get the list of all namespace prefixes
			Iterator<String> prefixIt = namespaceContext.getAllPrefixes();
			while (prefixIt.hasNext()) {
				String prefix = prefixIt.next();
				String namespaceURI = namespaceContext.getNamespaceURI(prefix);
				if (prefix == null ||  prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
					returnString += " xmlns=\"" + namespaceURI + "\"";
				} else {
					returnString += " xmlns:" + prefix + "=\"" + namespaceURI + "\"";
				}
			}
			
		}
		return returnString;
	}
	
	
	
	
	/**
	 * Convert the {@link com.att.research.xacml.api.Response} object into a string suitable for output in an HTTPResponse.
	 * This method generates the output without any pretty-printing.
	 * This is the method normally called by the Web Service for generating the output to the PEP through the RESTful interface.
	 * 
	 * @param response
	 * @param outputStream
	 * @throws java.io.IOException
	 * @throws DOMStructureException 
	 */
	public static void convert(Response response, OutputStream outputStream) throws IOException, DOMStructureException {
		convert(response, outputStream, false);
	}
	
	/**
	 * Do the work of converting the {@link com.att.research.xacml.api.Response} object to a string, allowing for pretty-printing if desired.
	 * 
	 * @param response
	 * @param outputStream
	 * @param prettyPrint
	 * @throws java.io.IOException
	 * @throws DOMStructureException 
	 */
	public static void convert(Response response, OutputStream outputStream, boolean prettyPrint) throws IOException, DOMStructureException {
		
		OutputStreamWriter osw = new OutputStreamWriter(outputStream);
		
		if (response == null) {
			throw new DOMStructureException("No Request in convert");
		}
		
		if (response.getResults() == null || response.getResults().size() == 0) {
			// must be at least one result
			throw new DOMStructureException("No Result in Response");
		}
		
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		
		if (prettyPrint) sb.append("\n");
		
		// response with attributes
		sb.append("<Response");
		
//TODO include all Namespace info
// Currently this is hard-coded for just the standard XACML namespaces, but ideally should use Namespaces from incoming Request to get non-standard ones.
		sb.append(" xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\"");
		sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		sb.append(" xsi:schemaLocation=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17");
        sb.append(" http://docs.oasis-open.org/xacml/3.0/xacml-core-v3-schema-wd-17.xsd\"");
        
        // end of <Response>
        sb.append(">");
				
		// for each Result...
		for (Result result : response.getResults()) {
			
			if (prettyPrint) sb.append("\n\t");
			
			sb.append("<Result>");
			
			// Decision
			if (prettyPrint) sb.append("\n\t\t");
			
			if (result.getDecision() == null) {
				throw new DOMStructureException("Result missing Decision");
			}
			sb.append("<Decision>" + result.getDecision().toString() + "</Decision>");
			
			// Status
			Status status = result.getStatus();
			if (status != null) {
				if (prettyPrint) sb.append("\n\t\t");
				sb.append("<Status>");
				
				// status code
				StatusCode statusCode = status.getStatusCode();
				Identifier statusCodeId;
				if (statusCode == null) {
					throw new DOMStructureException("Status must have StatusCode");
				} else {
					statusCodeId = statusCode.getStatusCodeValue();
		 			// if there is a status code, it must agree with the decision
	    			// Permit/Deny/NotAllowed must all be OK
	    			// Indeterminate must not be OK
	    			if ( (statusCodeId.equals(StdStatusCode.STATUS_CODE_OK.getStatusCodeValue())  &&
	    					! (result.getDecision() == Decision.DENY || result.getDecision() == Decision.PERMIT || result.getDecision() == Decision.NOTAPPLICABLE))  ||
	    				( ! statusCodeId.equals(StdStatusCode.STATUS_CODE_OK.getStatusCodeValue())  &&
	    	    					! (result.getDecision() == Decision.INDETERMINATE || result.getDecision() == Decision.INDETERMINATE_DENY || 
	    	    						result.getDecision() == Decision.INDETERMINATE_DENYPERMIT || result.getDecision() == Decision.INDETERMINATE_PERMIT ))  )
	    					{
	    				throw new DOMStructureException("StatusCode '" + statusCodeId.stringValue() + "' does not match Decision '" + result.getDecision().toString());
	    			}
					
					
					outputStatusCode(sb, statusCode, 3, prettyPrint);
				}
				
				// status message
				if (status.getStatusMessage() != null) {
	
					if (prettyPrint) sb.append("\n\t\t\t");
					sb.append("<StatusMessage>" + status.getStatusMessage() + "</StatusMessage>");

				}
				
				// status detail
				StatusDetail statusDetail = status.getStatusDetail();
				if (statusDetail != null) {
	    			// cross-check that rules defined in XACML Core spec section 5.5.7 re: when StatusDetail may/may-not be included have been followed
	    			if (status.isOk()) {
	    				throw new DOMStructureException("Status '" + statusCodeId.stringValue() + "' must not return StatusDetail");
	    			} else if (statusCodeId.stringValue().equals(XACML3.ID_STATUS_MISSING_ATTRIBUTE.stringValue()) && 
	    					status.getStatusDetail().getMissingAttributeDetails() == null) {
	    				throw new DOMStructureException("Status '" + statusCodeId.stringValue() + "' has StatusDetail without MissingAttributeDetail");
	    			} else if (statusCodeId.stringValue().equals(XACML3.ID_STATUS_SYNTAX_ERROR.stringValue()))  {
	    				throw new DOMStructureException("Status '" + statusCodeId.stringValue() + "' must not return StatusDetail");
	    			} else if (statusCodeId.stringValue().equals(XACML3.ID_STATUS_PROCESSING_ERROR.stringValue()))  {
	    				throw new DOMStructureException("Status '" + statusCodeId.stringValue() + "' must not return StatusDetail");
	    			}
	    			
	    			// if included, StatusDetail is handled differently for each type of detail message and the contents are formatted into escaped XML rather than objects
	    			
	    			if (result.getStatus().getStatusDetail().getMissingAttributeDetails() != null) {
						if (prettyPrint) sb.append("\n\t\t\t");
						sb.append("<StatusDetail>");
						
						for (MissingAttributeDetail mad : statusDetail.getMissingAttributeDetails()) {
							if (mad.getAttributeId() == null || mad.getCategory() == null || mad.getDataTypeId() == null) {
								throw new DOMStructureException("MissingAttributeDetail is missing required AttributeId, Category or DataTypeId");
							}
							if (prettyPrint) sb.append("\n\t\t\t\t");
							sb.append("<MissingAttributeDetail");
							sb.append(" Category=\"" + mad.getCategory().stringValue() + "\"");
							sb.append(" AttributeId=\"" + mad.getAttributeId().stringValue() + "\"");
							sb.append(" DataTypeId=\"" + mad.getDataTypeId().stringValue() + "\"");
							if (mad.getIssuer() != null) {
								sb.append(" Issuer=\"" + mad.getIssuer() + "\"");
							}
							sb.append(">");
							if (mad.getAttributeValues() != null) {
								for (AttributeValue<?> value : mad.getAttributeValues()) {
									if (prettyPrint) {
										sb.append("\n\t\t\t\t\t");
									}
									sb.append("<AttributeValue" + getNamespaces(value.getValue()) + ">" + outputValueValue(value.getValue()) + "</AttributeValue>");
								}
							}
							if (prettyPrint) {
								sb.append("\n\t\t\t\t");
							}
							sb.append("</MissingAttributeDetail>");
						}
						
						if (prettyPrint) {
							sb.append("\n\t\t\t");
						}
						sb.append("</StatusDetail>");
	    			}
				}
				
				if (prettyPrint) sb.append("\n\t\t");
				sb.append("</Status>");
			}
			
			// Obligations
			if (result.getObligations() != null && result.getObligations().size() > 0) {
				if (prettyPrint) sb.append("\n\t\t");
				sb.append("<Obligations>");
				
				for (Obligation obligation : result.getObligations()) {
					if (obligation.getId() == null) {
						throw new DOMStructureException("Obligation must have ObligationId");
					}
					if (prettyPrint) sb.append("\n\t\t\t");
					sb.append("<Obligation ObligationId=\"" + obligation.getId().stringValue() + "\">");
					
					for (AttributeAssignment aa : obligation.getAttributeAssignments()) {
						if (prettyPrint) sb.append("\n\t\t\t\t");
						sb.append("<AttributeAssignment");
						
						if (aa.getAttributeId() == null) {
							throw new DOMStructureException("Obligation AttributeAssignment must have AttributeId");
						}
						sb.append(" AttributeId=\"" + aa.getAttributeId().stringValue() + "\"");
						if (aa.getDataTypeId() == null || aa.getAttributeValue() == null || aa.getAttributeValue().getValue() == null) {
							throw new DOMStructureException("Obligation AttributeAssignment '" + aa.getAttributeId().stringValue() + "' must have DataType and Value");
						}
						sb.append(" DataType=\"" + aa.getDataTypeId().stringValue() + "\"" + getNamespaces(aa.getAttributeValue().getValue()) + ">");
						sb.append(outputValueValue(aa.getAttributeValue().getValue()));

						sb.append("</AttributeAssignment>");
					}
					
					if (prettyPrint) sb.append("\n\t\t\t");
					sb.append("</Obligation>");
				}
				
				if (prettyPrint) sb.append("\n\t\t");
				sb.append("</Obligations>");
			}
			
			// AssociatedAdvice
			if (result.getAssociatedAdvice() != null && result.getAssociatedAdvice().size() > 0) {
				if (prettyPrint) sb.append("\n\t\t");
				sb.append("<AssociatedAdvice>");
				
				for (Advice advice : result.getAssociatedAdvice()) {
					if (advice.getId() == null) {
						throw new DOMStructureException("Advice must have AdviceId");
					}
					if (prettyPrint) sb.append("\n\t\t\t");
					sb.append("<Advice AdviceId=\"" + advice.getId().stringValue() + "\">");
					
					for (AttributeAssignment aa : advice.getAttributeAssignments()) {
						if (prettyPrint) sb.append("\n\t\t\t\t");
						sb.append("<AttributeAssignment");
						
						if (aa.getAttributeId() == null) {
							throw new DOMStructureException("Advice AttributeAssignment must have AttributeId");
						}
						sb.append(" AttributeId=\"" + aa.getAttributeId().stringValue() + "\"");
						if (aa.getDataTypeId() == null || aa.getAttributeValue() == null || aa.getAttributeValue().getValue() == null) {
							throw new DOMStructureException("Advice AttributeAssignment '" + aa.getAttributeId().stringValue() + "' must have DataType and Value");
						}
						sb.append(" DataType=\"" + aa.getDataTypeId().stringValue() + "\"" + getNamespaces(aa.getAttributeValue().getValue()) + ">");
						sb.append(outputValueValue(aa.getAttributeValue().getValue()));

						sb.append("</AttributeAssignment>");
					}
					
					if (prettyPrint) sb.append("\n\t\t\t");
					sb.append("</Advice>");
				}
				
				if (prettyPrint) sb.append("\n\t\t");
				sb.append("</AssociatedAdvice>");
			}
			
			// Attributes
			if (result.getAttributes() != null && result.getAttributes().size() > 0) {
				// this may include attributes with IncludeInResult=false!

				
				for (AttributeCategory category : result.getAttributes()) {
					if (prettyPrint) sb.append("\n\t\t");
					if (category.getCategory() == null) {
						throw new DOMStructureException("Attributes must have Category");
					}
					sb.append("<Attributes Category=\"" + category.getCategory().stringValue() + "\">");
					
					for (Attribute attr : category.getAttributes()) {
						if (attr.getIncludeInResults() == false) {
							// skip this one - do not include in results
							continue;
						}
						if (prettyPrint) sb.append("\n\t\t\t");
						sb.append("<Attribute IncludeInResult=\""+ attr.getIncludeInResults()+"\"");
						if (attr.getAttributeId() == null) {
							throw new DOMStructureException("Attribute inf Category '" + category.getCategory().stringValue() + "' must have AttributeId");
						}
						sb.append(" AttributeId=\"" + attr.getAttributeId().stringValue() + "\"");
						if (attr.getIssuer() == null) {
							sb.append(">");
						} else {
							sb.append(" Issuer=\"" + attr.getIssuer() + "\">");
						}
						
						if (attr.getValues().size() == 0) {
							throw new DOMStructureException("Attribute '" + attr.getAttributeId() + "' must have at least one value");
						}
						for (AttributeValue<?> value : attr.getValues()) {
							if (value.getDataTypeId() == null || value.getValue() == null) {
								throw new DOMStructureException("Attribute '" + attr.getAttributeId() + "' has AttributeValue missing either DataType or Value");
							}
							if (prettyPrint) sb.append("\n\t\t\t\t");
							sb.append("<AttributeValue DataType=\"" + value.getDataTypeId().stringValue() + "\"");
							if (value.getXPathCategory() != null) {
								sb.append(" XPathCategory=\"" + value.getXPathCategory().stringValue() + "\"");
							}
							sb.append(">");
							
							sb.append(outputValueValue(value.getValue()));
							
							
							sb.append("</AttributeValue>");
						}
						
						if (prettyPrint) sb.append("\n\t\t\t");
						sb.append("</Attribute>");
					}
					
					if (prettyPrint) sb.append("\n\t\t");
					sb.append("</Attributes>");
				}
				
			}
			
			
			// PolicyIdentifierList
			Collection<IdReference> policyIds = result.getPolicyIdentifiers();
			Collection<IdReference> policySetIds = result.getPolicySetIdentifiers();
			if (policyIds != null && policyIds.size() > 0 || policySetIds != null && policySetIds.size() > 0) {
				if (prettyPrint) sb.append("\n\t\t\t");
				sb.append("<PolicyIdentifierList>");
				
				// individual Ids
				for (IdReference idReference : policyIds) {
					if (idReference == null) {
						throw new DOMStructureException("PolicyIdentifiers has null IdReference");
					}
					if (prettyPrint) sb.append("\n\t\t\t\t");
					sb.append("<PolicyIdReference");
					if (idReference.getVersion() != null) {
						sb.append(" Version=\"" + idReference.getVersion().stringValue() + "\">");
					} else {
						sb.append(">");
					}
					sb.append(idReference.getId().stringValue());
					sb.append("</PolicyIdReference>");		
				}
				// Set Ids
				for (IdReference idReference : policySetIds) {
					if (idReference == null) {
						throw new DOMStructureException("PolicySetIdentifiers has null IdReference");
					}
					if (prettyPrint) sb.append("\n\t\t\t\t");
					sb.append("<PolicySetIdReference");
					if (idReference.getVersion() != null) {
						sb.append(" Version=\"" + idReference.getVersion().stringValue() + "\">");
					} else {
						sb.append(">");
					}
					sb.append(idReference.getId().stringValue());
					sb.append("</PolicySetIdReference>");		
				}
				
				if (prettyPrint) sb.append("\n\t\t\t");
				sb.append("</PolicyIdentifierList>");
			}
			
			
			
			// end of Result
			if (prettyPrint) sb.append("\n\t");
			sb.append("</Result>");
		}
		
		if (prettyPrint) sb.append("\n");
		
		sb.append("</Response>");
		
		// all done
		
		osw.write(sb.toString());
		
		// force output
		osw.flush();

	}
	
	
	
	
	
	
	/**
	 * Unit test program to load an XML file containing a XACML Response document.
	 * 
	 * @param args the list of Response files to load and parse
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			DocumentBuilderFactory	documentBuilderFactory	= DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(true);
			for (String xmlFileName: args) {
				File	fileXml	= new File(xmlFileName);
				if (!fileXml.exists()) {
					System.err.println("Input file \"" + fileXml.getAbsolutePath() + "\" does not exist.");
					continue;
				} else if (!fileXml.canRead()) {
					System.err.println("Permission denied reading input file \"" + fileXml.getAbsolutePath() + "\"");
					continue;
				}
				System.out.println(fileXml.getAbsolutePath() + ":");
				try {
					DocumentBuilder	documentBuilder	= documentBuilderFactory.newDocumentBuilder();
					assert(documentBuilder.isNamespaceAware());
					Document documentResponse		= documentBuilder.parse(fileXml);
					assert(documentResponse != null);
					
					NodeList children				= documentResponse.getChildNodes();
					if (children == null || children.getLength() == 0) {
						System.err.println("No Responses found in \"" + fileXml.getAbsolutePath() + "\"");
						continue;
					} else if (children.getLength() > 1) {
						System.err.println("Multiple Responses found in \"" + fileXml.getAbsolutePath() + "\"");
					}
					Node nodeResponse				= children.item(0);
					if (!nodeResponse.getLocalName().equals(XACML3.ELEMENT_RESPONSE)) {
						System.err.println("\"" + fileXml.getAbsolutePath() + "\" is not a Response");
						continue;
					}
					
					Response domResponse			= DOMResponse.newInstance(nodeResponse);
					System.out.println(domResponse.toString());
					System.out.println();
				} catch (Exception ex) {
					ex.printStackTrace(System.err);
				}
			}
		}
	}


}
