package cmucoref.mention.featgen;

import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;
import cmucoref.util.Pair;
import cmucoref.mention.Dictionaries;

/**
 * 
 * @author Max
 *
 */

public class MentionFeatureGenerator {
    public MentionFeatureGenerator(){}
    
    /**
     * 
     * @param anaph
     * @param anaphSent
     * @param antec
     * @param antecSent
     * @param mode
     * @param dict
     * @param model
     * @param useEvent
     * @param mfv
     * @param efv
     */
    public void genCoreferentFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
            int mode, Dictionaries dict, CorefModel model, boolean useEvent, FeatureVector mfv, FeatureVector efv) {
        if(mode == 0) {
            //attribute match features
            genAttrMatchFeatures(anaph, anaphSent, antec, antecSent, dict, model, useEvent, mfv, efv);            
        }
        else if(mode == 1) {
            //string match features
            genStringMatchFeatures(anaph, anaphSent, antec, antecSent, dict, model, useEvent, mfv, efv);
        }
        else if(mode == 2) {
            //precise match features
            genPreciseMatchFeatures(anaph, anaphSent, antec, antecSent, dict, model, useEvent, mfv, efv);
        }
    }
    
    //-------------------------------------------------------------
    //precise match mode
    protected void genPreciseMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
            Dictionaries dict, CorefModel model, boolean useEvent, FeatureVector mfv, FeatureVector efv) {
        // new cluster features
        if(antec == null) {
            genPreciseMatchNewClusterFeatures(anaph, anaphSent, model, mfv, efv);
        }
        else {
            genPreciseMatchMentionFeatures(anaph, anaphSent, antec, antecSent, dict, model, mfv, efv);
            if(useEvent) {
                genPreciseMatchEventFeatures(anaph, anaphSent, antec, antecSent, dict, model, efv);
            }
        }
    }
    
    protected void genPreciseMatchMentionFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
            Dictionaries dict, CorefModel model, FeatureVector mfv, FeatureVector efv) {
        //given
        StringBuilder given = new StringBuilder();
        given.append("mode=PREC_MAT");
        given.append(", TYPE=" +  antec.getMentionType());
        
        //entry
        StringBuilder feat = new StringBuilder();
        feat.append("TYPE=" + anaph.getMentionType());
        
        addMentionFeature(feat.toString(), given.toString(), model, mfv);
    }
    
    protected void genPreciseMatchNewClusterFeatures(Mention anaph, Sentence anaphSent, 
            CorefModel model, FeatureVector mfv, FeatureVector efv) {
        throw new RuntimeException("precise match does not have new cluster case");
    }
    
    protected void genPreciseMatchEventFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
            Dictionaries dict, CorefModel model, FeatureVector efv) {
        // no event features
    }
    
    //--------------------------------------------------
    //string match mode
    protected void genStringMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
            Dictionaries dict, CorefModel model, boolean useEvent, FeatureVector mfv, FeatureVector efv) {
        
        // new cluster features
        if(antec == null) {
            genStringMatchNewClusterFeatures(anaph, anaphSent, model, mfv, efv);
        }
        else {
            genStringMatchMentionFeatures(anaph, anaphSent, antec, antecSent, dict, model, mfv, efv);
            if(useEvent) {
                genStringMatchEventFeatures(anaph, anaphSent, antec, antecSent, dict, model, efv);
            }
        }
    }
    
    protected void genStringMatchMentionFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
            Dictionaries dict, CorefModel model, FeatureVector mfv, FeatureVector efv) {
        if(anaph.isPronominal() || antec.isPronominal()) {
            throw new RuntimeException("string match feature does not apply to pronominals");
        }
        
        //given
        StringBuilder given = new StringBuilder();
        given.append("mode=STR_MAT");
        given.append(", TYPE=" +  antec.getMentionType());
        given.append(", DEF=" + antec.definite);
        
        //entry
        StringBuilder feat = new StringBuilder();
        feat.append("TYPE=" + anaph.getMentionType());
        
        //string match features
        if(anaph.isList() || antec.isList()) {
            feat.append(", " + "HDMAT=true");
            feat.append(", " + "CMPMOD=true");
            feat.append(", " + "WDIND=true");
            feat.append(", " + "RLXMAT=true");
            feat.append(", " + "EXTMAT=true");
        }
        else {
            boolean headMatch = antec.headMatch(antecSent, anaph, anaphSent, dict);
            feat.append(", " + "HDMAT=" + headMatch);
            
            //Acronym or Demonym
            boolean ADNYM = (Mention.options.useDemonym() && anaph.isDemonym(anaphSent, antec, antecSent, dict))
                    || anaph.acronymMatch(anaphSent, antec, antecSent, dict);
            
            //compatible modifier
            boolean compatibleModifier = ADNYM || antec.compatibleModifier(antecSent, anaph, anaphSent, dict);
            feat.append(", " + "CMPMOD=" + compatibleModifier);
            //wordInclude
            boolean wordIncluded = ADNYM || antec.wordsInclude(antecSent, anaph, anaphSent, dict);
            feat.append(", " + "WDIND=" + wordIncluded);
            //exact match
            boolean exactMatch = ADNYM || anaph.exactSpanMatch(anaphSent, antec, antecSent);
            //relaxed match
            boolean relaxedMatch = exactMatch || anaph.relaxedSpanMatch(anaphSent, antec, antecSent);
            
            feat.append(", " + "RLXMAT=" + relaxedMatch);
            feat.append(", " + "EXTMAT=" + exactMatch);
            
//            if(!compatibleModifier && exactMatch) {
//                System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%");
//                antec.display(antecSent, System.out);
//                anaph.display(anaphSent, System.out);
//            }
        }
        addMentionFeature(feat.toString(), given.toString(), model, mfv);
    }
    
    protected void genStringMatchNewClusterFeatures(Mention anaph, Sentence anaphSent, 
            CorefModel model, FeatureVector mfv, FeatureVector efv) {
        throw new RuntimeException("string match does not have new cluster case");
    }
    
    protected void genStringMatchEventFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
            Dictionaries dict, CorefModel model, FeatureVector efv) {
        // get event feature of antec
        String antecFeat = null;
        if(antec.getMainEvent() != null) {
            antecFeat = antec.getMainEvent().toFeature();
        }
        // get event feature of anaph
        String anaphFeat = null;
        if(anaph.getMainEvent() != null) {
            anaphFeat = anaph.getMainEvent().toFeature();
        }
        addEventFeature(anaphFeat, antecFeat, model, efv);
    }
    
    //--------------------------------------------------
    //attribute match mode
    protected void genAttrMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
            Dictionaries dict, CorefModel model, boolean useEvent, FeatureVector mfv, FeatureVector efv) {
        
        if(antec == null) {
            genAttrMatchMentionNewClusterFeatures(anaph, anaphSent, model, useEvent, mfv, efv);
            if(useEvent) {
                genAttrMatchEventNewClusterEventFeatures(anaph, anaphSent, model, efv);
            }
        }
        else {
            genAttrMatchMentionFeatures(anaph, anaphSent, antec, antecSent, dict, model, mfv, efv);
            if(useEvent) {
                genAttrMatchEventFeatures(anaph, anaphSent, antec, antecSent, dict, model, efv);
            }
        }
    }
    
    protected void genAttrMatchMentionFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
            Dictionaries dict, CorefModel model, FeatureVector mfv, FeatureVector efv) {
        //given
        StringBuilder given = new StringBuilder();
        given.append("mode=ATTR_MAT");
        
        //type feature
        given.append(", TYPE=" +  antec.getMentionType());
        //definite feature
        given.append(", DEF=" + antec.definite);
        //number feature
        given.append(", NUM=" + antec.number);
        //animacy feature
        given.append(", ANI=" + antec.animacy);
        //gender feature
        given.append(", GEN=" + antec.gender);
        //person feature
        given.append(", PER=" + antec.person);
        
        
        //entry
        StringBuilder feat = new StringBuilder();
        //distance features
        int distOfSent = anaph.getDistOfSent(antec);
        feat.append("DIST=" + distOfSent);
        //type features
        feat.append(", TYPE=" + anaph.getMentionType());
        //number features;
        feat.append(", NUM=" + anaph.number);
        //animacy features
        feat.append(", ANI=" + anaph.animacy);
        //gender features
        feat.append(", GEN=" + anaph.gender);
        //person feature
        feat.append(", PER=" + anaph.person);
        
        //string match features (does not apply for pronominals)
//        if(!anaph.isPronominal() && !antec.isPronominal()) {
//            if(anaph.isList() || antec.isList()) {
//                boolean headMatch = antec.headMatch(antecSent, anaph, anaphSent, dict);
//                feat.append(", " + "HDMAT=" + headMatch);
//                feat.append(", " + "CMPMOD=false");
//                feat.append(", " + "WDIND=false");
//            }
//            else {
//                boolean headMatch = antec.headMatch(antecSent, anaph, anaphSent, dict);
//                feat.append(", " + "HDMAT=" + headMatch);
//                //compatible modifier
//                boolean compatibleModifier = antec.compatibleModifier(antecSent, anaph, anaphSent, dict);
//                feat.append(", " + "CMPMOD=" + compatibleModifier);
//                //wordInclude
//                boolean wordIncluded = antec.wordsInclude(antecSent, anaph, anaphSent, dict);
//                feat.append(", " + "WDIND=" + wordIncluded);
//            }
//        }        
        addMentionFeature(feat.toString(), given.toString(), model, mfv);
    }
    
    protected void genAttrMatchMentionNewClusterFeatures(Mention anaph, Sentence anaphSent,
            CorefModel model, boolean useEvent, FeatureVector mfv, FeatureVector efv) {

        //given
        String given = "mode=ATTR_MAT, NEWCLUSMENTION";
        
        //entry
        StringBuilder feat = new StringBuilder();
        feat.append("TYPE=" + anaph.getMentionType());
        //number features;
        feat.append(", NUM=" + anaph.number);
        //animacy features
        feat.append(", ANI=" + anaph.animacy);
        //gender features
        feat.append(", GEN=" + anaph.gender);
        //person features
        feat.append(", PER=" + anaph.person);
        addMentionFeature(feat.toString(), given, model, mfv);
    }
    
    protected void genAttrMatchEventFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
            Dictionaries dict, CorefModel model, FeatureVector efv) {
        // get event feature of antec
        String antecFeat = null;
        if(antec.getMainEvent() != null) {            
            antecFeat = antec.getMainEvent().toFeature();
        }
        // get event feature of anaph
        String anaphFeat = null;
        if(anaph.getMainEvent() != null) {
            anaphFeat = anaph.getMainEvent().toFeature();
        }
        addEventFeature(anaphFeat, antecFeat, model, efv);
    }
    
    public void genAttrMatchEventNewClusterEventFeatures(Mention anaph, Sentence anaphSent, 
            CorefModel model, FeatureVector efv) {
        String given = "NEWCLUSEVENT";
        String anaphFeat = null;
        if(anaph.getMainEvent() != null) {
            anaphFeat = anaph.getMainEvent().toFeature();
        }
        addEventFeature(anaphFeat, given, model, efv);
    }
    
    //----------------------------------------------------------
    
    protected final void addMentionFeature(String feat, String given, CorefModel model, FeatureVector mfv){
        Pair<Integer, Integer> ids = model.getMentionFeatureIndex(feat, given);
        if(mfv != null){
            mfv.addFeature(ids.first, ids.second);
        }
    }
    
    protected final void addEventFeature(String feat, String given, CorefModel model, FeatureVector efv){
        Pair<Integer, Integer> ids = model.getEventFeatureIndex(feat, given);
        if(efv != null){
            efv.addFeature(ids.first, ids.second);
        }
    }
}
