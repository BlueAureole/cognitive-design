package xyz.zhiwei.cognitivedesign.morphism.support.image;

import java.util.List;

import xyz.zhiwei.cognitivedesign.morphism.Principle;

/**
 * 本原段
 */
public class PrincipleImage {
	
	private List<List<? extends Principle<?>>> addition;
	private List<List<? extends Principle<?>>> modification;
	private List<List<? extends Principle<?>>> deletion;
	
	public PrincipleImage() {}
	

	public PrincipleImage(List<List<? extends Principle<?>>> addition) {
		this.addition=addition;
	}
	public PrincipleImage(List<List<? extends Principle<?>>> addition,List<List<? extends Principle<?>>> modification) {
		this.addition=addition;
		this.modification=modification;
	}
	public PrincipleImage(List<List<? extends Principle<?>>> addition,List<List<? extends Principle<?>>> modification,List<List<? extends Principle<?>>> deletion) {
		this.addition=addition;
		this.modification=modification;
		this.deletion=deletion;
	}
	
	
	
	
	public List<List<? extends Principle<?>>> getAddition() {
		return addition;
	}
	public void setAddition(List<List<? extends Principle<?>>> addition) {
		this.addition = addition;
	}
	public List<List<? extends Principle<?>>> getModification() {
		return modification;
	}
	public void setModification(List<List<? extends Principle<?>>> modification) {
		this.modification = modification;
	}
	public List<List<? extends Principle<?>>> getDeletion() {
		return deletion;
	}
	public void setDeletion(List<List<? extends Principle<?>>> deletion) {
		this.deletion = deletion;
	}

}
