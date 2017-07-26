<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="/">

    <xsl:for-each select="SubmissionStatus">
      submissionId=<xsl:value-of select="@submission_id"/>
      <xsl:for-each select="Action">
        action
      </xsl:for-each>


    </xsl:for-each>

  </xsl:template>

</xsl:stylesheet>