import java.util.ArrayList;


public class FaceDisplay {
	public static final int cActors = 6; // number of characters in the working set
	private ActorFace[] faces = new ActorFace[cActors];
	private ArrayList<Expression> expressions = new ArrayList<Expression>();
	
	public FaceDisplay() {	
		expressions = XMLHandler.loadExpressions();
		// generate the small versions of the features
		for (Expression e:expressions) {
			for (int i=0; i<Expression.FeatureCount; ++i) {
				e.getFeature(i).makeItSmall();
			}
		}
		faces = XMLHandler.loadActors();
		for (ActorFace af:faces) {
			af.makeItSmall();
		}
	}
	public Expression getExpression(int tIndex) {
		return expressions.get(tIndex);
	}
	public ArrayList<Expression> getExpressions() {
		return expressions;
	}
	public ActorFace getFace(int tIndex) {
		return faces[tIndex];
	}
	public int getExpressionCount() {
		return expressions.size();
	}
}
