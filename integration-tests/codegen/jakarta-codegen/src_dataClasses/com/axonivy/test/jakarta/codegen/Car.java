package com.axonivy.test.jakarta.codegen;

/**
 */
@SuppressWarnings("all")
@javax.annotation.processing.Generated(comments="This is the java file of the ivy data class Car", value={"ch.ivyteam.ivy.scripting.streamInOut.IvyScriptJavaClassBuilder"})
@jakarta.persistence.Entity
public class Car extends ch.ivyteam.ivy.scripting.objects.CompositeObject
{
  /** SerialVersionUID */
  private static final long serialVersionUID = 3150185062730109042L;

  public static com.axonivy.test.jakarta.codegen.CarRepo repository() {
    return ch.ivyteam.ivy.environment.Ivy.persistence().repository(com.axonivy.test.jakarta.codegen.CarRepo.class);
  }

  /**
   * Identifier
   */
  @jakarta.persistence.Id
  @jakarta.persistence.GeneratedValue
  private java.lang.Integer id;

  /**
   * Gets the field id.
   * @return the value of the field id; may be null.
   */
  public java.lang.Integer getId()
  {
    return id;
  }

  /**
   * Sets the field id.
   * @param _id the new value of the field id.
   */
  public void setId(java.lang.Integer _id)
  {
    id = _id;
  }

  private java.lang.String brand;

  /**
   * Gets the field brand.
   * @return the value of the field brand; may be null.
   */
  public java.lang.String getBrand()
  {
    return brand;
  }

  /**
   * Sets the field brand.
   * @param _brand the new value of the field brand.
   */
  public void setBrand(java.lang.String _brand)
  {
    brand = _brand;
  }

  private java.lang.String model;

  /**
   * Gets the field model.
   * @return the value of the field model; may be null.
   */
  public java.lang.String getModel()
  {
    return model;
  }

  /**
   * Sets the field model.
   * @param _model the new value of the field model.
   */
  public void setModel(java.lang.String _model)
  {
    model = _model;
  }

  private java.lang.Integer release;

  /**
   * Gets the field release.
   * @return the value of the field release; may be null.
   */
  public java.lang.Integer getRelease()
  {
    return release;
  }

  /**
   * Sets the field release.
   * @param _release the new value of the field release.
   */
  public void setRelease(java.lang.Integer _release)
  {
    release = _release;
  }

}
