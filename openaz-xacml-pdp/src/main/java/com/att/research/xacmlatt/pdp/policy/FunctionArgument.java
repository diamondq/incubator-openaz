/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */

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
package com.att.research.xacmlatt.pdp.policy;

import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.Status;

/**
 * FunctionArgument is the interface implemented by objects that can serve as arguments to a {@link com.att.research.xacmlatt.pdp.policy.FunctionDefinition}
 * <code>evaluate</code> call.
 * 
 * @author car
 * @version $Revision: 1.3 $
 */
public interface FunctionArgument {
	/**
	 * Gets the {@link com.att.research.xacml.api.Status} from the evaluation of this <code>FunctionArgument</code>.
	 * 
	 * @return the <code>Status</code> from the evaluation of this <code>FunctionArgument</code>>
	 */
	public Status getStatus();
	
	/**
	 * Determines if this <code>FunctionArgument</code> is OK and can have its <code>AttributeValue</code> or
	 * <code>Bag</code> retrieved.
	 * 
	 * @return true if this <code>FunctionArgument</code> is OK, otherwise false.
	 */
	public boolean isOk();
	
	/**
	 * Determines if this <code>FunctionArgument</code> represents a bag of values.
	 * 
	 * @return true if this <code>FunctionArgument</code> represents a bag of values, else false.
	 */
	public boolean isBag();
	
	/**
	 * Gets the single <code>AttributeValue</code> representing the value of this <code>FunctionArgument</code>.  If
	 * this <code>FunctionArgument</code> represents a bag, the value returned is up to the implementation.
	 * 
	 * @return the single <code>AttributeValue</code> representing the value of this <code>FunctionArgument</code>.
	 */
	public AttributeValue<?> getValue();
	
	/**
	 * Gets the {@link Bag} value for this <code>FunctionArgument</code> if the
	 * argument represents a <code>Bag</code>, (i.e. <code>isBag</code> returns true).
	 * 
	 * @return the <code>Bag</code> value for this <code>FunctionArgument</code>.
	 */
	public Bag getBag();
	
}