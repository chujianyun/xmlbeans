/*   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xmlbeans.impl.jam.annogen.provider;

/**
 * <p>Simple identifier for an annotation type.  It describes a
 * a bi-directional mapping between a public 'declared type' and the
 * annogen-generated 'proxy type.'
 *
 *
 * @author Patrick Calahan &lt;email: pcal-at-bea-dot-com&gt;
 */
public class AnnoType {

  // ========================================================================
  // Variables

  private Class mDeclared;
  private Class mProxy;

  // ========================================================================
  // Constructors

  public AnnoType(Class declared, Class proxy) {
    if (declared == null) throw new IllegalArgumentException("null declared");
    if (proxy == null) throw new IllegalArgumentException("null proxy");
    mDeclared = declared;
    mProxy = proxy;
  }

  // ========================================================================
  // Public methods

  /**
   * <p>Returns the public, declared type of the annotation.  The class
   * returned will usually be a 175 annotation type.</p>
   */
  public Class getDeclaredClass() { return mDeclared; }

  /**
   * <p>Returns proxy type which corresponds to the given declared type.
   * This typically is an 'impl' class that was generated by annogen.</p>
   */
  public Class getProxyClass() { return mProxy; }


}
