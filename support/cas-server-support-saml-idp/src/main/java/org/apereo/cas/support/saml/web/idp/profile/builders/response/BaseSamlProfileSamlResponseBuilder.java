package org.apereo.cas.support.saml.web.idp.profile.builders.response;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.support.saml.OpenSamlConfigBean;
import org.apereo.cas.support.saml.SamlException;
import org.apereo.cas.support.saml.SamlProtocolConstants;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.services.idp.metadata.SamlRegisteredServiceServiceProviderMetadataFacade;
import org.apereo.cas.support.saml.util.AbstractSaml20ObjectBuilder;
import org.apereo.cas.support.saml.web.idp.profile.builders.SamlProfileObjectBuilder;
import org.apereo.cas.support.saml.web.idp.profile.builders.enc.SamlObjectEncrypter;
import org.apereo.cas.support.saml.web.idp.profile.builders.enc.SamlObjectSigner;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.velocity.VelocityEngineFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The {@link BaseSamlProfileSamlResponseBuilder} is responsible for
 * building the final SAML assertion for the relying party.
 *
 * @author Misagh Moayyed
 * @since 4.2
 */
public abstract class BaseSamlProfileSamlResponseBuilder<T extends XMLObject>
        extends AbstractSaml20ObjectBuilder implements SamlProfileObjectBuilder {
    private static final long serialVersionUID = -1891703354216174875L;

    /**
     * The Saml object encoder.
     */
    protected SamlObjectSigner samlObjectSigner;

    /**
     * The Velocity engine factory.
     */
    protected VelocityEngineFactory velocityEngineFactory;

    @Autowired
    private CasConfigurationProperties casProperties;

    private SamlProfileObjectBuilder<Assertion> samlProfileSamlAssertionBuilder;

    private SamlObjectEncrypter samlObjectEncrypter;

    public BaseSamlProfileSamlResponseBuilder(final OpenSamlConfigBean openSamlConfigBean,
                                              final SamlObjectSigner samlObjectSigner,
                                              final VelocityEngineFactory velocityEngineFactory,
                                              final SamlProfileObjectBuilder<Assertion> samlProfileSamlAssertionBuilder,
                                              final SamlObjectEncrypter samlObjectEncrypter) {
        super(openSamlConfigBean);
        this.samlObjectSigner = samlObjectSigner;
        this.velocityEngineFactory = velocityEngineFactory;
        this.samlProfileSamlAssertionBuilder = samlProfileSamlAssertionBuilder;
        this.samlObjectEncrypter = samlObjectEncrypter;
    }

    @Override
    public T build(final AuthnRequest authnRequest,
                   final HttpServletRequest request,
                   final HttpServletResponse response,
                   final org.jasig.cas.client.validation.Assertion casAssertion,
                   final SamlRegisteredService service,
                   final SamlRegisteredServiceServiceProviderMetadataFacade adaptor) throws SamlException {
        final Assertion assertion = buildSamlAssertion(authnRequest, request, response, casAssertion, service, adaptor);
        final T finalResponse = buildResponse(assertion, casAssertion, authnRequest, service, adaptor, request, response);
        return encodeFinalResponse(request, response, service, adaptor, finalResponse);
    }

    /**
     * Encode final response.
     *
     * @param request       the request
     * @param response      the response
     * @param service       the service
     * @param adaptor       the adaptor
     * @param finalResponse the final response
     * @return the response
     */
    protected T encodeFinalResponse(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final SamlRegisteredService service,
                                    final SamlRegisteredServiceServiceProviderMetadataFacade adaptor,
                                    final T finalResponse) {
        final String relayState = request.getParameter(SamlProtocolConstants.PARAMETER_SAML_RELAY_STATE);
        logger.debug("RelayState is [{}]", relayState);
        return encode(service, finalResponse, response, adaptor, relayState);
    }

    /**
     * Build saml assertion assertion.
     *
     * @param authnRequest the authn request
     * @param request      the request
     * @param response     the response
     * @param casAssertion the cas assertion
     * @param service      the service
     * @param adaptor      the adaptor
     * @return the assertion
     */
    protected Assertion buildSamlAssertion(final AuthnRequest authnRequest,
                                           final HttpServletRequest request,
                                           final HttpServletResponse response,
                                           final org.jasig.cas.client.validation.Assertion casAssertion,
                                           final SamlRegisteredService service,
                                           final SamlRegisteredServiceServiceProviderMetadataFacade adaptor) {
        return this.samlProfileSamlAssertionBuilder.build(authnRequest, request, response, casAssertion, service, adaptor);
    }


    /**
     * Build response response.
     *
     * @param assertion    the assertion
     * @param casAssertion the cas assertion
     * @param authnRequest the authn request
     * @param service      the service
     * @param adaptor      the adaptor
     * @param request      the request
     * @param response     the response
     * @return the response
     * @throws SamlException the saml exception
     */
    protected abstract T buildResponse(Assertion assertion,
                                       org.jasig.cas.client.validation.Assertion casAssertion,
                                       AuthnRequest authnRequest,
                                       SamlRegisteredService service,
                                       SamlRegisteredServiceServiceProviderMetadataFacade adaptor,
                                       HttpServletRequest request,
                                       HttpServletResponse response) throws SamlException;

    /**
     * Build entity issuer issuer.
     *
     * @return the issuer
     */
    protected Issuer buildEntityIssuer() {
        final Issuer issuer = newIssuer(casProperties.getAuthn().getSamlIdp().getEntityId());
        issuer.setFormat(Issuer.ENTITY);
        return issuer;
    }

    /**
     * Encode the final result into the http response.
     *
     * @param service      the service
     * @param samlResponse the saml response
     * @param httpResponse the http response
     * @param adaptor      the adaptor
     * @param relayState   the relay state
     * @return the t
     * @throws SamlException the saml exception
     */
    protected abstract T encode(SamlRegisteredService service,
                                T samlResponse,
                                HttpServletResponse httpResponse,
                                SamlRegisteredServiceServiceProviderMetadataFacade adaptor,
                                String relayState) throws SamlException;

    /**
     * Encrypt assertion.
     *
     * @param assertion the assertion
     * @param request   the request
     * @param response  the response
     * @param service   the service
     * @param adaptor   the adaptor
     * @return the saml object
     * @throws SamlException the saml exception
     */
    protected SAMLObject encryptAssertion(final Assertion assertion,
                                          final HttpServletRequest request, final HttpServletResponse response,
                                          final SamlRegisteredService service,
                                          final SamlRegisteredServiceServiceProviderMetadataFacade adaptor) throws SamlException {
        try {
            if (service.isEncryptAssertions()) {
                logger.info("SAML service [{}] requires assertions to be encrypted", adaptor.getEntityId());
                final EncryptedAssertion encryptedAssertion =
                        this.samlObjectEncrypter.encode(assertion, service, adaptor, response, request);
                return encryptedAssertion;
            }
            logger.info("SAML registered service [{}] does not require assertions to be encrypted", adaptor.getEntityId());
            return assertion;
        } catch (final Exception e) {
            throw new SamlException("Unable to marshall assertion for encryption", e);
        }
    }
}
