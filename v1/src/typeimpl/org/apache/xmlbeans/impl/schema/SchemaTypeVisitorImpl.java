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

package org.apache.xmlbeans.impl.schema;

import org.apache.xmlbeans.impl.values.TypeStoreVisitor;
import org.apache.xmlbeans.impl.values.TypeStore;
import javax.xml.namespace.QName;
import org.apache.xmlbeans.SchemaParticle;
import org.apache.xmlbeans.SchemaLocalElement;
import org.apache.xmlbeans.SchemaField;

import java.util.Arrays;

/**
 * This state machine validates element order based on a deterministic
 * content model.
 */
public class SchemaTypeVisitorImpl implements TypeStoreVisitor
{
    /**
     * Construct it by passing the root of the content model.
     */
    public SchemaTypeVisitorImpl(SchemaParticle part)
    {
        init(part);
    }

    public SchemaTypeVisitorImpl()
    {

    }

    public void init(SchemaParticle part)
    {
        if (_stack == null)
        {
            _stack = expand(null);
        }
        if (_rollback == null)
        {
            _rollback = expand(null);
        }

        _stackSize = 0;
        _rollbackSize = 0;

        if (part != null)
        {
            push(part);
            _rollbackIndex = 1;
        }
    }

    public VisitorState[] expand(VisitorState[] orig)
    {
        int newsize = (orig == null ? 4 : orig.length * 2);
        VisitorState[] result = new VisitorState[newsize];
        if (orig != null)
            System.arraycopy(orig, 0, result, 0, orig.length);
        for (int i = (orig == null ? 0 : orig.length); i < newsize; i++)
        {
            result[i] = new VisitorState();
        }
        return result;
    }


    final static boolean PROBE_VALIDITY = true;
    final static boolean CHECK_VALIDITY = false;

    private VisitorState[] _stack;
    private VisitorState[] _rollback;
    int _stackSize;
    int _rollbackSize;
    private boolean _isValid;
    private SchemaParticle _matchedParticle;
    private VisitorState _top;
    private int _rollbackIndex;

    private static class VisitorState
    {
        public void copy(VisitorState orig)
        {
            _curPart = orig._curPart;
            _curCount = orig._curCount;
            _curMin = orig._curMin;
            _curMax = orig._curMax;
            _processedChildCount = orig._processedChildCount;
            _childCount = orig._childCount;
            _seen = orig._seen;
        }

        public void init(SchemaParticle part)
        {
            _curPart = part;
            _curMin = part.getIntMinOccurs();
            _curMax = part.getIntMaxOccurs();
            _curCount = 0;
            _processedChildCount = 0;
            _childCount = part.countOfParticleChild();
            _seen = part.getParticleType() == SchemaParticle.ALL ?
                        new boolean[_childCount] : null;
        }

        SchemaParticle _curPart;
        int _curCount;
        int _curMax;
        int _curMin;
        int _processedChildCount;
        int _childCount;
        boolean[] _seen;
    }

    VisitorState topRef()
    {
        return _stack[_stackSize - 1];
    }

    void saveCopy(VisitorState ref)
    {
        if (_rollback.length == _rollbackSize)
            _rollback = expand(_rollback);

        _rollback[_rollbackSize].copy(ref);
        _rollbackSize += 1;
    }

    void addParticle(SchemaParticle part)
    {
        if (_stack.length == _stackSize)
            _stack = expand(_stack);
        _stack[_stackSize].init(part);
        _stackSize += 1;
    }

    /**
     * Precondition:
     * stack:    (R1 R2 R3 R4)
     * top:      null or discardable due to pop in progress
     * rollback: (R8 R7 R6 R5) (empty at the start of "visit")
     *
     * Postcondition:
     * stack:    (R1 R2 R3)
     * top:      D4 = mutable copy of R4
     * rollback  (R8 R7 R6 R5 R4)
     */
    boolean prepare()
    {
        if (_rollbackIndex == 0)
        {
            _top = null;
            return false;
        }

        _top = topRef();
        saveCopy(_top);
        _rollbackIndex = _stackSize - 1;
        return true;
    }

    /**
     * Precondition:
     * stack:    (R1 R2 R3 R4 D5 D6)
     * top:      D7
     * rollback: (R8 R7 R6 R5)
     *
     * Postcondition:
     * stack:    (R1 R2 R3 R4 D5 D6 D7)
     * top:      D8 = new state based on part
     * rollback  (R8 R7 R6 R5)
     */
    void push(SchemaParticle part)
    {
        addParticle(part);
        _top = topRef();
    }

    /**
     * Precondition:
     * stack:    (R1 R2 R3 R4 D5 D6)
     * top:      D7 is discardable
     * rollback: (R8 R7 R6 R5)
     *
     * Postcondition:
     * stack:    (R1 R2 R3 R4 D5)
     * top:      D6 (see prepare in case it's an R state)
     * rollback  (R8 R7 R6 R5)
     */
    boolean pop()
    {
        _stackSize -= 1;
        if (_stackSize <= _rollbackIndex)
            return prepare();
        _top = topRef();
        return true;
    }

    /**
     * Precondition:
     * stack:    (R1 R2 R3 R4 D5 D6)
     * top:      D7
     * rollback: (R8 R7 R6 R5)
     *
     * Postcondition:
     * stack:    (R1 R2 R3 R4 D5 D6 D7) -> and rename to D's to R's
     * top:      null
     * rollback  ()
     */
    void commit()
    {
        _top = null;
        _rollbackIndex = _stackSize;
        _rollbackSize = 0;
    }

    /**
     * Precondition:
     * stack:    (R1 R2 R3 R4 D5 D6)
     * top:      D7 is discardable
     * rollback: (R8 R7 R6 R5)
     *
     * Postcondition:
     * stack:    (R1 R2 R3 R4 R5 R6 R7 R8)
     * top:      null
     * rollback  ()
     */
    void rollback()
    {
        while (_rollbackSize > 0)
        {
            _rollbackSize -= 1;
            VisitorState temp = _stack[_rollbackIndex];
            _stack[_rollbackIndex] = _rollback[_rollbackSize];
            _rollback[_rollbackSize] = temp;
            _rollbackIndex += 1;
        }
        _stackSize = _rollbackIndex;
        _top = null;
    }

    /**
     * When no valid next state can be found, then "notValid"
     * is returned. It rolls back any changes to the state
     * machine stack, sets to false the valid bit, and clears
     * the matched element state before returning false.
     */
    boolean notValid()
    {
        _isValid = false;
        _matchedParticle = null;
        rollback();
        return false;
    }

    /**
     * When a valid state transition has been done, then "ok"
     * is returned. It commits the changed state machine state,
     * stores the matched element state, and returns true.
     */
    boolean ok(SchemaParticle part, boolean testValidity)
    {
        if ( ! testValidity )
        {
            _matchedParticle = part;
            commit();
        }
        else
        {
            rollback();
        }
        return true;
    }

    /*== VISITOR IMPLEMENTATION ==*/

    /**
     * Traverses a deterministic content model, checking for
     * validity at any given point.
     *
     * Call visit(null) once at the end if you're checking for
     * complete validity of the sequence of elements.
     *
     * This is a wrapper for the actual visit implementation.
     */
    public boolean visit(QName eltName)
    {
      return visit(eltName, CHECK_VALIDITY);
    }

    /**
     * The actual implementation that
     * traverses a deterministic content model, checking for
     * validity at any given point.
     *
     * When testValidity is false then this method will change states
     * if the current state is valid
     *
     * When testValidity is true then this method will not change states
     * and will return if a particular state is valid or invalid
     */

    public boolean visit(QName eltName ,boolean testValidity)
    {
        if (!prepare())
            return notValid();

        traversing: for (;;)
        {
            while (_top._curCount >= _top._curMax)
            {
                if (!pop())
                    break traversing;
            }

            minmax: switch (_top._curPart.getParticleType())
            {
                default:
                    assert(false);

                case SchemaParticle.WILDCARD:
                    if (!_top._curPart.canStartWithElement(eltName))
                    {
                        if (_top._curCount < _top._curMin)
                            return notValid();
                        break minmax;
                    }
                    _top._curCount++;
                    return ok(_top._curPart, testValidity);

                case SchemaParticle.ELEMENT:
                    if (!_top._curPart.canStartWithElement(eltName))
                    {
                        if (_top._curCount < _top._curMin)
                            return notValid();
                        break minmax;
                    }
                    _top._curCount++;
                    return ok(_top._curPart, testValidity);

                case SchemaParticle.SEQUENCE:
                    for (int i = _top._processedChildCount; i < _top._childCount; i++)
                    {
                        SchemaParticle candidate = _top._curPart.getParticleChild(i);
                        if (candidate.canStartWithElement(eltName))
                        {
                            _top._processedChildCount = i + 1;
                            push(candidate);
                            continue traversing;
                        }
                        if (!candidate.isSkippable())
                        {
                            if (_top._processedChildCount != 0 || _top._curCount < _top._curMin)
                                return notValid();
                            break minmax;
                        }
                    }
                    _top._curCount++;
                    _top._processedChildCount = 0;
                    continue traversing;

                case SchemaParticle.CHOICE:
                    for (int i = 0; i < _top._childCount; i++)
                    {
                        SchemaParticle candidate = _top._curPart.getParticleChild(i);
                        if (candidate.canStartWithElement(eltName))
                        {
                            _top._curCount++;
                            push(candidate);
                            continue traversing;
                        }
                    }
                    if (_top._curCount < _top._curMin && !_top._curPart.isSkippable())
                        return notValid();
                    break minmax;

                case SchemaParticle.ALL:

                    int skipped = _top._processedChildCount;
                    allscan: for (int i = 0; i < _top._childCount; i++)
                    {
                        if (_top._seen[i])
                            continue allscan;

                        SchemaParticle candidate = _top._curPart.getParticleChild(i);
                        if (candidate.canStartWithElement(eltName))
                        {
                            _top._processedChildCount++;
                            _top._seen[i] = true;
                            push(candidate);
                            continue traversing;
                        }
                        else if (candidate.isSkippable())
                        {
                            skipped += 1;
                        }
                    }
                    if (skipped < _top._childCount)
                    {
                        if (_top._curCount < _top._curMin)
                            return notValid();
                        break minmax;
                    }
                    _top._curCount++;
                    _top._processedChildCount = 0;
                    Arrays.fill(_top._seen, false);
                    continue traversing;
            }

            // somebody called "break minmax", so pop out of loop
            if (!pop())
                break traversing;
        }

        // we've completed the outermost loop
        if (eltName == null)
            return ok(null, testValidity);

        // this means we have extra elements at the end
        return notValid();
    }

    public boolean testValid(QName eltName)
    {
      return visit(eltName,PROBE_VALIDITY);
    }

    /**
     * Constructs elementflags
     */
    public int get_elementflags()
    {
        if (currentParticle() == null || currentParticle().getParticleType() != SchemaParticle.ELEMENT)
            return 0;

        SchemaLocalElement elt = (SchemaLocalElement)currentParticle();

        return (elt.isNillable() ? TypeStore.NILLABLE : 0) |
               (elt.isDefault() ? TypeStore.HASDEFAULT : 0) |
               (elt.isFixed() ? TypeStore.FIXED : 0);
    }

    /**
     * Returns default text
     */
    public String get_default_text()
    {
        if (currentParticle() == null || currentParticle().getParticleType() != SchemaParticle.ELEMENT)
            return null;

        return ((SchemaLocalElement)currentParticle()).getDefaultText();
    }

    /**
     * Returns the schema field for this field.
     */
    public SchemaField get_schema_field() {
        if (currentParticle() instanceof SchemaField)
            return (SchemaField)currentParticle();
        
        return null;
    }

    /**
     * Returns the current schema element
     */
    public SchemaParticle currentParticle()
    {
        return _matchedParticle;
    }

    /**
     * Returns true if the entire content up to now is valid.
     */
    public boolean isAllValid()
    {
        return _isValid;
    }
}
