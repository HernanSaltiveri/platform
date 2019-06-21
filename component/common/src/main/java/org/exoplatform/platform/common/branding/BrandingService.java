package org.exoplatform.platform.common.branding;

import java.io.IOException;

public interface BrandingService {

  Branding getBrandingInformation();

  void updateBrandingInformation(Branding branding) throws Exception;

  String getCompanyName();

  void updateCompanyName(String companyName);

  String getTopbarTheme();

  Long getLogoId();

  void updateTopbarTheme(String style);

  void uploadLogo(Logo logo) throws IOException, Exception;

}
