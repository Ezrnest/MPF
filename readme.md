# MPF
A rule based formal proof assistant.

## Introduction

**This project is still in its early stage, we welcome
any interested collaborator.** 

MPF is designed to provide proof assistance of logic and math basing on 
fundamental logic rules. It provides utilities for logic formulas,
formula matches, rules and more. In addition, it also enables users
to write proofs and verify them. 




Currently, fundamental components including formula, matcher and logic rules 
are completed. First order logic rules are supported. 

### Term and Formula
Term and formula are basic components of first order logic. They are both represented with tree structures.
The corresponding super classes are `core.Term` and `core.Formula`. 

Term consists of variables, constants and functions. The pre-defined concrete subclasses are 
 `VarTerm`, `ConstTerm`, `FunTerm` and `NamedTerm`. Users can extend the class `Term` for specific usage.

Formula is composed of predicates of terms and logic conjunctions. The recursive definitions and 
the corresponding subclasses of formula are:
* Predicate with terms `p(t1, t2, ... tn)`: `PredicateFormula`
* Logic not of a formula `¬P` : `NotFormula`
* Two formulas associated by logic conjunctions 'imply', 'equivalent to', 'and' and 'or'
 `P→Q`,`P↔Q`,`P∧Q`,`P∨Q`: `ImplyFormula`, `EquivToFormula`, `AndFormula`, `OrFormula`.
* A formula with quantifier 'any' or 'exist' `∀xP(x)`, `∃xP(x)` : `ForAnyFormula`, `ExistFormula`

The `Formula` class also provides some useful methods for manipulating formulas, including recursive iteration,
recursive mapping and variable renaming. Please refer to the documentation for more details.

### Matchers
Matchers are provided for matching formulas(or terms) structurally and making replacements. They are powerful
tools with which most of the general logic rules are constructed. Matchers are in the package `matcher`. 




## Deduction System

(To be written)

## Examples

See the folder samples for MPF proofs in Kotlin DSL.

## Dependencies
* Java 14
* Kotlin 1.3


