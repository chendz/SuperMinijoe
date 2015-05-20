// Copyright 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.minijoe.compiler.ast;

import com.google.minijoe.compiler.CompilerException;
import com.google.minijoe.compiler.Visitor;

/**
 * @author Andy Hayward
 */
public class TryStatement extends Statement {
  public Statement tryBlock;
  public Identifier catchIdentifier;
  public Statement catchBlock;
  public Statement finallyBlock;

  public TryStatement(
      Statement tryBlock,
      Identifier catchIdentifier,
      Statement catchBlock,
      Statement finallyBlock) {
    this.tryBlock = tryBlock;
    this.catchIdentifier = catchIdentifier;
    this.catchBlock = catchBlock;
    this.finallyBlock = finallyBlock;
  }

  public Statement visitStatement(Visitor visitor) throws CompilerException {
    return visitor.visit(this);
  }
}
