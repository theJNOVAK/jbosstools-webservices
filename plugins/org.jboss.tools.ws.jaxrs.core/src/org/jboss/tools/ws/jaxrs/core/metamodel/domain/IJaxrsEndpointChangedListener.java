/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.metamodel.domain;


/**
 * Interface to get notified of the JAX-RS Endpoint changes.
 * 
 * @author xcoulon
 * 
 */
public interface IJaxrsEndpointChangedListener {


	/**
	 * Method called when a JAX-RS Endpoint changed (ADDED, CHANGED or REMOVED)
	 * in the Metamodel.
	 * 
	 * @param delta
	 */
	public void notifyEndpointChanged(JaxrsEndpointDelta delta);

}