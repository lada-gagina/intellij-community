/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.groovy.lang.psi.api.statements;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import org.jetbrains.plugins.groovy.lang.psi.GroovyRecursiveElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrIndexProperty;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;

import java.util.*;

/**
 * User: Dmitry.Krasilschikov
 * Date: 02.06.2009
 */
public class GrNamedArgumentSearchVisitor extends GroovyRecursiveElementVisitor {
  private static final Set<String> METHOD_NAMES = new HashSet<String>(Arrays.asList("containsKey", "remove", "get"));

  private Set<String> myResult;

  private final String myFirstArgumentName;

  public GrNamedArgumentSearchVisitor(String firstArgumentName) {
    myFirstArgumentName = firstArgumentName;
  }

  public String[] getResult() {
    return myResult == null ? ArrayUtil.EMPTY_STRING_ARRAY : myResult.toArray(new String[myResult.size()]);
  }

  private void extractArguments(GrArgumentList argumentList) {
    GrExpression[] expr = argumentList.getExpressionArguments();

    if (expr.length == 1 && expr[0] instanceof GrLiteral) {
      Object value = ((GrLiteral)expr[0]).getValue();
      if (value instanceof String) {
        String s = (String)value;
        if (StringUtil.isJavaIdentifier(s)) {
          add((String)value);
        }
      }
    }
  }

  private void add(String refName) {
    if (myResult == null) {
      myResult = new HashSet<String>();
    }

    myResult.add(refName);
  }

  @Override
  public void visitReferenceExpression(GrReferenceExpression referenceExpression) {
    if (myFirstArgumentName.equals(referenceExpression.getName()) && !referenceExpression.isQualified()) {
      PsiElement parent = referenceExpression.getParent();

      if (parent instanceof GrReferenceExpression) {
        GrReferenceExpression parentRef = (GrReferenceExpression)parent;

        PsiElement parentParent = parentRef.getParent();

        if (parentParent instanceof GrMethodCallExpression) {
          if (METHOD_NAMES.contains(parentRef.getName())) {
            extractArguments(((GrMethodCallExpression)parentParent).getArgumentList());
          }
        }
        else {
          add(parentRef.getName());
        }
      }
      else if (parent instanceof GrIndexProperty) {
        GrIndexProperty indexProperty = (GrIndexProperty)parent;
        extractArguments(indexProperty.getArgumentList());
      }
    }

    super.visitReferenceExpression(referenceExpression);
  }
}
