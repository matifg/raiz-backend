package com.raiz.bakcend.service;

import com.raiz.bakcend.config.AppProperties;
import com.raiz.bakcend.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VerificacionEmailService {

    private static final Logger log = LoggerFactory.getLogger(VerificacionEmailService.class);

    private final EmailService emailService;
    private final AppProperties appProperties;

    public VerificacionEmailService(EmailService emailService, AppProperties appProperties) {
        this.emailService = emailService;
        this.appProperties = appProperties;
    }

    public void enviarEmailVerificacion(Usuario usuario, UUID token) {
        String frontendUrl = appProperties.getFrontend().getUrl();
        if (frontendUrl == null || frontendUrl.isBlank()) {
            throw new IllegalStateException("FRONTEND_URL no configurada");
        }
        frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;

        String verificationUrl = frontendUrl + "/verificar-email?token=" + token;
        log.info("[VERIFY_EMAIL] Enviando mail to={} token={} url={}",
                usuario.getEmail(), token, verificationUrl);

        String nombre = usuario.getNombre();
        String saludo = (nombre != null && !nombre.isBlank())
                ? "¡Hola, " + nombre.trim() + "! 👋"
                : "¡Hola! 👋";
        String emailCuenta = usuario.getEmail() != null ? usuario.getEmail() : "";

        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Verificá tu cuenta de Inmo360</title>
                </head>
                <body style="margin:0;padding:0;background-color:#F5F7FA;font-family:Arial,Helvetica,sans-serif;-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%;">
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:#F5F7FA;margin:0;padding:0;">
                    <tr>
                      <td align="center" style="padding:40px 16px;">
                        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;background-color:#ffffff;border-radius:12px;box-shadow:0 4px 24px rgba(15,23,42,0.08);overflow:hidden;">
                          <tr>
                            <td style="padding:48px 40px 32px 40px;text-align:center;">
                              <p style="margin:0 0 8px 0;font-size:32px;line-height:1.2;font-weight:700;color:#111827;letter-spacing:-0.5px;">Inmo360</p>
                              <p style="margin:0;font-size:15px;line-height:1.6;color:#6B7280;">La plataforma para profesionales inmobiliarios.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 40px;">
                              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="border-top:1px solid #E5E7EB;font-size:0;line-height:0;">&nbsp;</td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:36px 40px 0 40px;">
                              <p style="margin:0 0 16px 0;font-size:18px;line-height:1.4;font-weight:600;color:#111827;">{saludo}</p>
                              <h1 style="margin:0 0 20px 0;font-size:24px;line-height:1.3;font-weight:700;color:#111827;">¡Bienvenido a Inmo360!</h1>
                              <p style="margin:0 0 16px 0;font-size:16px;line-height:1.7;color:#374151;">Gracias por registrarte en Inmo360.</p>
                              <p style="margin:0 0 16px 0;font-size:16px;line-height:1.7;color:#374151;">Estamos muy contentos de que formes parte de nuestra comunidad.</p>
                              <p style="margin:0 0 16px 0;font-size:16px;line-height:1.7;color:#374151;">Este enlace corresponde a la cuenta <strong>{emailCuenta}</strong>.</p>
                              <p style="margin:0 0 32px 0;font-size:16px;line-height:1.7;color:#374151;">Para activar tu cuenta solo necesitás confirmar tu dirección de correo electrónico haciendo clic en el botón de abajo.</p>
                              <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center" style="margin:0 auto 20px auto;">
                                <tr>
                                  <td align="center" bgcolor="#5B3DF5" style="border-radius:8px;background-color:#5B3DF5;">
                                    <a href="{verificationUrl}" target="_blank" style="display:inline-block;padding:14px 32px;font-size:16px;line-height:1.2;font-weight:700;color:#ffffff;text-decoration:none;border-radius:8px;background-color:#5B3DF5;">Verificar mi cuenta</a>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:0 0 32px 0;font-size:14px;line-height:1.6;color:#6B7280;text-align:center;">Este enlace será válido durante las próximas 24 horas.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 40px;">
                              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="border-top:1px solid #E5E7EB;font-size:0;line-height:0;">&nbsp;</td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px 40px 40px 40px;">
                              <p style="margin:0 0 12px 0;font-size:13px;line-height:1.6;color:#6B7280;">Si el botón no funciona, copiá y pegá este enlace en tu navegador:</p>
                              <p style="margin:0;font-size:13px;line-height:1.6;word-break:break-all;">
                                <a href="{verificationUrl}" target="_blank" style="color:#5B3DF5;text-decoration:underline;">{verificationUrl}</a>
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 40px 40px 40px;">
                              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="border-top:1px solid #E5E7EB;font-size:0;line-height:0;">&nbsp;</td>
                                </tr>
                              </table>
                              <p style="margin:24px 0 8px 0;font-size:13px;line-height:1.6;color:#6B7280;text-align:center;">¿No solicitaste esta cuenta?</p>
                              <p style="margin:0 0 24px 0;font-size:13px;line-height:1.6;color:#6B7280;text-align:center;">Podés ignorar este correo de forma segura.</p>
                              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="border-top:1px solid #E5E7EB;font-size:0;line-height:0;">&nbsp;</td>
                                </tr>
                              </table>
                              <p style="margin:24px 0 8px 0;font-size:12px;line-height:1.6;color:#9CA3AF;text-align:center;">© 2026 Inmo360</p>
                              <p style="margin:0;font-size:12px;line-height:1.6;color:#9CA3AF;text-align:center;">La plataforma para profesionales inmobiliarios.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.replace("{verificationUrl}", verificationUrl)
                .replace("{saludo}", saludo)
                .replace("{emailCuenta}", emailCuenta);

        String subject = "Verificá tu cuenta de Inmo360 (" + emailCuenta + ")";
        emailService.sendHtmlEmail(usuario.getEmail(), subject, html);
    }
}
