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
package org.apache.xmlbeans.impl.jam.internal.java15;

import org.apache.xmlbeans.impl.jam.mutable.MAnnotatedElement;
import org.apache.xmlbeans.impl.jam.mutable.MAnnotation;
import org.apache.xmlbeans.impl.jam.internal.javadoc.Javadoc15Delegate;
import org.apache.xmlbeans.impl.jam.internal.javadoc.JavadocClassBuilder;
import org.apache.xmlbeans.impl.jam.internal.elements.ElementContext;
import org.apache.xmlbeans.impl.jam.JClass;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.AnnotationTypeElementDoc;
import com.sun.javadoc.ExecutableMemberDoc;



/**
 * @author Patrick Calahan &lt;email: pcal-at-bea-dot-com&gt;
 */
public class Javadoc15DelegateImpl implements Javadoc15Delegate {

  // ========================================================================
  // Variables

  private ElementContext mContext = null;

  // ========================================================================
  // Javadoc15Delegate implementation

  public void init(ElementContext ctx) {
    if (mContext != null) {
      throw new IllegalStateException("init called more than once");
    }
    mContext = ctx;
  }

  public void extractAnnotations(MAnnotatedElement dest,
                                 ProgramElementDoc src) {
    if (mContext == null) throw new IllegalStateException("init not called");
    if (dest == null) throw new IllegalArgumentException("null dest");
    if (src == null) throw new IllegalArgumentException("null src");
    extractAnnotations(dest,src.annotations(),src.position());
  }

  public void extractAnnotations(MAnnotatedElement dest,
                                 ExecutableMemberDoc method,
                                 Parameter src)
  {
    extractAnnotations(dest,src.annotations(),method.position());
  }

  public boolean isEnum(ClassDoc cd) {
    return cd.isEnum();
  }

  // ========================================================================
  // Private methods

  private void extractAnnotations(MAnnotatedElement dest,
                                  AnnotationDesc[] anns,
                                  SourcePosition sp)
  {
    if (anns == null) return; //?
    for(int i=0; i<anns.length; i++) {
      MAnnotation destAnn = dest.findOrCreateAnnotation
        (anns[i].annotationType().asClassDoc().qualifiedName());
      populateAnnotation(destAnn,anns[i],sp);
    }
  }

  private void populateAnnotation(MAnnotation dest, 
                                  AnnotationDesc src, 
                                  SourcePosition sp) {
    if (sp != null) JavadocClassBuilder.addSourcePosition(dest,sp);
    AnnotationDesc.ElementValuePair[] mvps = src.elementValues();
    for(int i=0; i<mvps.length; i++) {
      Type jmt = mvps[i].element().returnType();
      String typeName = jmt.qualifiedTypeName();
      String name = mvps[i].element().name();
      AnnotationValue aval = mvps[i].value();
      Object valueObj;
      try {
        valueObj = aval.value();
      } catch(NullPointerException npe) {
        //FIXME temporary workaround for sun bug
        mContext.getLogger().warning
          ("Encountered a known javadoc bug which usually \n"+
           "indicates a syntax error in an annotation value declaration.\n"+
           "The value is being ignored.\n"+
           "[file="+sp.file()+", line="+sp.line()+"]");
        continue;

      }
      if (mContext.getLogger().isVerbose(this)) {
        mContext.getLogger().verbose(name+" is a "+typeName+" with valueObj "+
                                     valueObj+", class is "+valueObj.getClass());
      }
      // ok, take a look at how what it really is and translate it into an
      // appropriate represenatation
      if (valueObj instanceof AnnotationDesc) {
        MAnnotation nested = dest.createNestedValue(name,typeName);
        populateAnnotation(nested,(AnnotationDesc)valueObj,sp);
      } else if (valueObj instanceof Number || valueObj instanceof Boolean) {
        JClass type = mContext.getClassLoader().loadClass(jmt.typeName());
        dest.setSimpleValue(name,valueObj,type);
      } else if (valueObj instanceof FieldDoc) {
        // this means it's an enum constant
        JClass type = mContext.getClassLoader().loadClass
          (((FieldDoc)valueObj).containingClass().qualifiedName());
        String val = ((FieldDoc)valueObj).name(); //REVIEW is this right?
        dest.setSimpleValue(name,val,type);
      } else if (valueObj instanceof ClassDoc) {
         JClass clazz = mContext.getClassLoader().loadClass
          (((FieldDoc)valueObj).containingClass().qualifiedName());
        dest.setSimpleValue(name,clazz,loadClass(JClass.class));
      } else if (valueObj instanceof String) {
        String v = ((String)valueObj).trim();
        if (v.startsWith("\"") && v.endsWith("\"")) {
          //javadoc gives us the quotes, which seems kinda dumb.  just deal.
          valueObj = v.substring(1,v.length()-1);
        }
        dest.setSimpleValue(name,valueObj,loadClass(String.class));
      } else if (valueObj instanceof AnnotationValue[]) {
        // this is another big chunk of work, just factored into a new
        // method to keep things cleaner
        populateArrayMember(dest,mvps[i].element(),(AnnotationValue[])valueObj,sp);
      } else {
        mContext.getLogger().error("Value of annotation member "+name+" is " +
                                   "of an unexpected type: "+
                                   valueObj.getClass()+"   ["+valueObj+"]");
      }
    }
  }

  /**
   *
   * The javadocs for com.sun.javadoc.AnnotationValue.value() read as follows:
   *
   * <p><pre><i>
   *
   * Returns the value. The type of the returned object is one of the
   * following:
   *
   * a wrapper class for a primitive type
   * String
   * ClassDoc
   * FieldDoc (representing an enum constant)
   * AnnotationDesc
   * AnnotationValue[]
   *
   * </i></pre></p>
   *
   * <p>It seems quite broken to me that in the array case, it returns an array
   * of AnnotationValues.  It would be a lot easier to deal with the API
   * if that last line instead read "or an array of any of the above."                                           
   * As it is, it's imposible to get the doclet API to give you a simple
   * array of ints, for example.  It's not at all clear what the extra
   * wrapping buys you.</p>
   *
   * <p>So, this method does a bunch of work so that JAM does the unwrapping
   * for the user.</p>
   */
  private void populateArrayMember(MAnnotation dest,
                                   AnnotationTypeElementDoc memberDoc,
                                   AnnotationValue[] annValueArray,
                                   SourcePosition sp)
  {
    if (sp != null) JavadocClassBuilder.addSourcePosition(dest,sp);
    String memberName = memberDoc.name();
    Type returnType = memberDoc.returnType();
    if (annValueArray.length == 0) {
      Object[] value = new Object[0];
      //FIXME this is a little bit busted - we should try to give them
      //more type information than this.  it's just a little bit harder
      //to figure it out from the javadoc objects
      dest.setSimpleValue(memberName,value,
                          loadClass(returnType.qualifiedTypeName()));
      return;
    }
    // unpack the AnnotationValue values into a single array.
    Object[] valueArray = new Object[annValueArray.length];
    for(int i=0; i<valueArray.length; i++) {
      try {
        valueArray[i] = annValueArray[i].value();
        if (valueArray[i] == null) {
          mContext.getLogger().error
            ("Javadoc provided an array annotation member value which contains "+
             "[file="+sp.file()+", line="+sp.line()+"]");
          return;
        }
      } catch(NullPointerException npe) {
        //FIXME temporary workaround for sun bug
        mContext.getLogger().warning
          ("Encountered a known javadoc bug which usually \n"+
           "indicates a syntax error in an annotation value declaration.\n"+
           "The value is being ignored.\n"+
           "[file="+sp.file()+", line="+sp.line()+"]");
        return;
      }
    }
    // now go do something with them
    if (valueArray[0] instanceof AnnotationDesc) {
      String annType =
        ((AnnotationDesc)valueArray[0]).annotationType().qualifiedName();
      MAnnotation[] anns = dest.createNestedValueArray
        (memberName, annType, valueArray.length);
      for(int i=0; i<anns.length; i++) {
        populateAnnotation(anns[i],(AnnotationDesc)valueArray[i],sp);
      }
    } else if (valueArray[0] instanceof Number || valueArray[0] instanceof Boolean) {
      JClass type = loadClass(JavadocClassBuilder.getFdFor(returnType));
      dest.setSimpleValue(memberName,annValueArray,type);
    } else if (valueArray[0] instanceof FieldDoc) {
      // this means it's an array of an enum constants
      String enumTypeName =
        ((FieldDoc)valueArray[0]).containingClass().qualifiedName();
      JClass memberType = loadClass("[L"+enumTypeName+";");
      String[] value = new String[valueArray.length];
      for(int i=0; i<valueArray.length; i++) {
        value[i] = ((FieldDoc)valueArray[i]).name(); //REVIEW is this right?
      }
      dest.setSimpleValue(memberName,value,memberType);
    } else if (valueArray[0] instanceof ClassDoc) {
      JClass[] value = new JClass[valueArray.length];
      for(int i=0; i<value.length; i++) {
        value[i] = loadClass(((ClassDoc)valueArray[0]).qualifiedName());
      }
      dest.setSimpleValue(memberName,value,loadClass(JClass[].class));
    } else if (valueArray[0] instanceof String) {
      String[] value = new String[valueArray.length];
      for(int i=0; i<value.length; i++) {
        String v = ((String)valueArray[i]).trim();
        if (v.startsWith("\"") && v.endsWith("\"")) {
          //javadoc gives us the quotes, which seems kinda dumb.  just deal.
          v = v.substring(1,v.length()-1);
        }
        value[i] = v;
      }
      dest.setSimpleValue(memberName,value,loadClass(String[].class));
    } else {
      mContext.getLogger().error("Value of array annotation member "+
                                 memberName+" is of an unexpected type: "+
                                 valueArray[0].getClass()+"   ["+
                                 valueArray[0]+"]");
    }
  }

  private JClass loadClass(String fd) {
    return mContext.getClassLoader().loadClass(fd);
  }

  //maybe we should put this on JamClassLoader?
  private JClass loadClass(Class clazz) {
    return loadClass(clazz.getName());
  }
}