package edu.stanford.nlp.pipeline;

import java.util.Collection;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.util.Pair;


public class DeprecatedAnnotations {

  @Deprecated
  public static class WordsPLAnnotation implements CoreAnnotation<List<List<? extends CoreLabel>>> {
    @SuppressWarnings({"unchecked", "RedundantCast"})
    public Class<List<List<? extends CoreLabel>>> getType() {  return (Class) List.class; } }


  @Deprecated
  public static class QuestionTypePLAnnotation implements CoreAnnotation<String> {
    public Class<String> getType() {  return String.class; } }


  @Deprecated
  public static class ParsePLAnnotation implements CoreAnnotation<List<Tree>> {
    @SuppressWarnings({"unchecked", "RedundantCast"})
    public Class<List<Tree>> getType() {  return (Class) List.class; } }


  @Deprecated
  public static class ParseKBestPLAnnotation implements CoreAnnotation<ClassicCounter<List<Tree>>> {
    @SuppressWarnings({"unchecked", "RedundantCast"})
    public Class<ClassicCounter<List<Tree>>> getType() {  return (Class) ClassicCounter.class; } }


  @Deprecated
  public static class CorefPLAnnotation implements CoreAnnotation<String> {
    public Class<String> getType() {  return String.class; } }


  @Deprecated
  public static class DependencyGraphPLAnnotation implements CoreAnnotation<List<SemanticGraph>> {
    @SuppressWarnings({"unchecked", "RedundantCast"})
    public Class<List<SemanticGraph>> getType() {  return (Class) List.class; } }


  @Deprecated
  public static class UncollapsedDependencyGraphPLAnnotation implements CoreAnnotation<List<SemanticGraph>> {
    @SuppressWarnings({"unchecked", "RedundantCast"})
    public Class<List<SemanticGraph>> getType() {  return (Class) List.class; } }


  @Deprecated
  public static class SRLPLAnnotation implements CoreAnnotation<List<List<Pair<String, Pair<Integer, Integer>>>>> {
    @SuppressWarnings({"unchecked", "RedundantCast"})
    public Class<List<List<Pair<String, Pair<Integer, Integer>>>>> getType() {  return (Class) List.class; } }


  @Deprecated
  public static class KBestNERsPLAnnotation implements CoreAnnotation<ClassicCounter<List<List<? extends CoreLabel>>>> {
    @SuppressWarnings({"unchecked", "RedundantCast"})
    public Class<ClassicCounter<List<List<? extends CoreLabel>>>> getType() {  return (Class) ClassicCounter.class; } }

}