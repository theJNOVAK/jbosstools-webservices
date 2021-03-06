/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.HTTP_METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.AnnotationUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.jdt.FlagsUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

/**
 * A request method designator is a runtime annotation that is annotated with
 * the @HttpMethod annotation. JAX-RS defines a set of request method
 * designators for the common HTTP resourceMethods: @GET, @POST, @PUT, @DELETE,
 * 
 * @HEAD. Users may define their own custom request method designators including
 *        alternate designators for the common HTTP resourceMethods.
 * 
 * @author xcoulon
 */
public class JaxrsHttpMethod extends JaxrsJavaElement<IType> implements IJaxrsHttpMethod {

	/**
	 * A Simple sorter for HTTP Verbs: the preferred order is
	 * GET/POST/PUT/DELETE/HEAD/OPTION. Other/custom Verbs come after.
	 */
	private enum HttpVerbSortEnum {
		/** GET Verb. */
		GET(0),
		/** POST Verb. */
		POST(1),
		/** PUT Verb. */
		PUT(2),
		/** DELETE Verb. */
		DELETE(3),
		/** HEAD Verb. */
		HEAD(5),
		/** OPTIONS Verb. */
		OPTIONS(6),
		/** Any other HTTP Verb. */
		OTHER(7);

		/** the order rank. */
		private int rank = 0;

		/**
		 * The constructor.
		 * 
		 * @param r
		 *            the rank
		 */
		HttpVerbSortEnum(final int r) {
			this.rank = r;
		}

		/** @return the rank */
		int getRank() {
			return rank;
		}

		/**
		 * Converter from literal to enum value. If no direct match, 'OTHER' is
		 * returned.
		 * 
		 * @param verb
		 *            the HTTP verb
		 * @return the corresponding value in the enum.
		 */
		static HttpVerbSortEnum from(final String verb) {
			for (HttpVerbSortEnum e : HttpVerbSortEnum.values()) {
				if (verb.equals(e.toString())) {
					return e;
				}
			}
			return OTHER;
		}
	}

	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IJavaElement javaElement) throws JavaModelException {
		final CompilationUnit ast = JdtUtils.parse(javaElement, new NullProgressMonitor());
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast);
		}
		return null;
	}

	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @param ast
	 *            the associated AST
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IJavaElement javaElement, final CompilationUnit ast) {
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast);
		}
		return null;
	}

	/**
	 * Internal Builder
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final IType javaType;
		private final CompilationUnit ast;
		private JaxrsMetamodel metamodel = null;
		private Map<String, Annotation> annotations;

		private Builder(final IType javaType, final CompilationUnit ast) {
			this.javaType = javaType;
			this.ast = ast;
		}

		public Builder withMetamodel(final JaxrsMetamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}

		public JaxrsHttpMethod build() throws CoreException {
			return build(true);
		}
		
		JaxrsHttpMethod build(final boolean joinMetamodel) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				if (javaType == null || !javaType.exists() || !javaType.isStructureKnown()) {
					return null;
				}
				JdtUtils.makeConsistentIfNecessary(javaType);
				this.annotations = JdtUtils.resolveAllAnnotations(javaType, ast);
				// Element *MUST* at least have the @HttpMethod annotation to be an HTTP Method.
				// Problems will be reported by validation if other annotations are missing.
				if (annotations == null || annotations.isEmpty() || !annotations.containsKey(HTTP_METHOD)) {
					return null;
				}
				final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(this);
				// this operation is only performed after creation
				if(joinMetamodel) {
					httpMethod.joinMetamodel();
				}
				return httpMethod;
			} finally {
				final long end = System.currentTimeMillis();
				Logger.tracePerf("Built JAX-RS HTTP Method in {}ms", (end - start));
			}

		}
	}

	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder
	 * 
	 */
	private JaxrsHttpMethod(final Builder builder) {
		this(builder.javaType, builder.annotations, builder.metamodel, null);
	}

	/**
	 * Full constructor that can be reused by {@link JaxrsBuiltinHttpMethod}
	 * 
	 * @param javaType
	 *            the underlying java type.
	 * @param annotations
	 *            the relevant annotations.
	 * @param metamodel
	 *            the metamodel or <code>null</code> if the instance is
	 *            transient.
	 * @param primaryCopy
	 *            the associated primary copy element, or {@code null} if this
	 *            instance is already the primary element
	 */
	protected JaxrsHttpMethod(final IType javaType, final Map<String, Annotation> annotations,
			final JaxrsMetamodel metamodel, final JaxrsHttpMethod primaryCopy) {
		super(javaType, annotations, metamodel, primaryCopy);
	}

	public boolean isBuiltIn() {
		return false;
	}

	@Override
	public JaxrsHttpMethod createWorkingCopy() {
		synchronized (this) {
			return new JaxrsHttpMethod(getJavaElement(), AnnotationUtils.createWorkingCopies(getAnnotations()),
					getMetamodel(), this);
		}
	}
	
	@Override
	public JaxrsHttpMethod getWorkingCopy() {
		return (JaxrsHttpMethod) super.getWorkingCopy();
	}
	
	@Override
	public String getJavaClassName() {
		if(getJavaElement() != null) {
			return getJavaElement().getFullyQualifiedName();
		}
		return null;
	}
	/**
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be a {@link JaxrsHttpMethod} anymore) 
	 */
	@Override
	boolean isMarkedForRemoval() {
		// element should be removed if HttpMethod annotation is missing
		return (getHttpMethodAnnotation() == null);
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.IHttpMethod#getHttpVerb
	 *      ()
	 */
	@Override
	public String getHttpVerb() {
		final Annotation httpVerbAnnotation = getHttpMethodAnnotation();
		if (httpVerbAnnotation != null) {
			return httpVerbAnnotation.getValue();
		}
		return null;
	}

	/** @return the HttpMethod Annotation */
	public Annotation getHttpMethodAnnotation() {
		return getAnnotation(HTTP_METHOD);
	}

	/** @return the Retention Annotation */
	public Annotation getRetentionAnnotation() {
		return getAnnotation(Retention.class.getName());
	}

	/** @return the Target Annotation */
	public Annotation getTargetAnnotation() {
		return getAnnotation(Target.class.getName());
	}

	@Override
	public final int compareTo(final IJaxrsHttpMethod other) {
		if (this.getHttpVerb() == null) {
			return 1;
		}
		final HttpVerbSortEnum enum1 = HttpVerbSortEnum.from(this.getHttpVerb());
		final HttpVerbSortEnum enum2 = HttpVerbSortEnum.from(other.getHttpVerb());
		// for known HTTP Verbs: use pre-defined ordering
		if(enum1.getRank() != enum2.getRank()) {
			return enum1.getRank() - enum2.getRank();
		}
		// for custom HTTP Verbs, compare the upper case value of the verbs
		return this.getHttpVerb().toUpperCase().compareTo(other.getHttpVerb().toUpperCase());
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.HTTP_METHOD;
	}

	/**
	 * Update this HttpMethod with the elements of the given element
	 * 
	 * @param element the {@link IJavaElement} to use to update {@code this} HTTP Method
	 * @param ast the {@link ICompilationUnit} associated with the given element
	 * @throws CoreException
	 */
	@Override
	public void update(final IJavaElement element, final CompilationUnit ast) throws CoreException {
		synchronized (this) {
			final Flags annotationsFlags = FlagsUtils.computeElementFlags(this);
			final JaxrsHttpMethod transientHttpMethod = JaxrsHttpMethod.from(element, ast).build(false);
			if (transientHttpMethod == null) {
				remove(annotationsFlags);
			} else {
				final Flags updateAnnotationsFlags = updateAnnotations(transientHttpMethod.getAnnotations());
				if (isMarkedForRemoval()) {
					remove(annotationsFlags);
				}
				// update indexes for this element.
				else if(hasMetamodel()){
					final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateAnnotationsFlags);
					getMetamodel().update(delta);
				}
			}
		}
	}

}
