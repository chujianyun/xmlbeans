/*
* The Apache Software License, Version 1.1
*
*
* Copyright (c) 2003 The Apache Software Foundation.  All rights
* reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by the
*        Apache Software Foundation (http://www.apache.org/)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Apache" and "Apache Software Foundation" must
*    not be used to endorse or promote products derived from this
*    software without prior written permission. For written
*    permission, please contact apache@apache.org.
*
* 5. Products derived from this software may not be called "Apache
*    XMLBeans", nor may "Apache" appear in their name, without prior
*    written permission of the Apache Software Foundation.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*
* This software consists of voluntary contributions made by many
* individuals on behalf of the Apache Software Foundation and was
* originally based on software copyright (c) 2000-2003 BEA Systems
* Inc., <http://www.bea.com/>. For more information on the Apache Software
* Foundation, please see <http://www.apache.org/>.
*/
package org.apache.xmlbeans.impl.binding.tylar;

import java.net.URI;
import org.apache.xmlbeans.impl.binding.bts.BindingFile;
import org.apache.xmlbeans.impl.binding.bts.BindingLoader;
import org.apache.xmlbeans.impl.jam.JClassLoader;
import org.apache.xmlbeans.SchemaTypeSystem;
import org.w3.x2001.xmlSchema.SchemaDocument;

/**
 * Abstract representation of a type library archive.  This is the interface
 * which is used by the binding runtime for retrieving information about a
 * tylar.
 *
 * @author Patrick Calahan <pcal@bea.com>
 */
public interface Tylar {

  // ========================================================================
  // Public methods

  /**
   * Returns a short textual description of this tylar.  This is primarily
   * useful for logging and debugging.
   */
  public String getDescription();

  /**
   * Returns a URI describing the location of the physical store from
   * which this Tylar was loaded.  This is useful for logging purposes.
   */
  public URI getLocation();

  /**
   * Returns the binding files contained in this Tylar.
   */
  public BindingFile[] getBindingFiles();

  /**
   * Returns the schema documents contained in this Tylar.
   */
  public SchemaDocument[] getSchemas();

  /**
   * Returns a BindingLoader for the bindings in this tylar.  This is really
   * just a convenience method; it simply returns a composite of the binding
   * files returned by getBindingFiles() plus the BuiltinBindingLoader.
   */
  public BindingLoader getBindingLoader();

  /**
   * Returns a BindingLoader for the bindings in this tylar.  This is really
   * just a convenience method; it simply returns a the schema type system
   * that results from compiling all of the schemas returned by getSchemas()
   * plus the BuiltinSchemaTypeSystem.
   */
  public SchemaTypeSystem getSchemaTypeSystem();


  /**
   * Returns a JClassLoader which can be used to load descriptions of the
   * java types contained in this tylar.
   */
  public JClassLoader getJClassLoader();

  /**
   * Returns a new ClassLoader that can load any class files contained in
   * this tylar.  Returns null if this tylar contains no class resources.
   *
   * REVIEW are we sure this method is needed?
   *
   * @param parent The parent for new classloader.
   */
  public ClassLoader createClassLoader(ClassLoader parent);
}