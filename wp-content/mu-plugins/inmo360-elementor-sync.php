<?php
/**
 * Plugin Name: Inmo360 Elementor Sync
 * Description: Fuerza el pipeline Document::save de Elementor y regenera post_content desde el mismo array $elements (evita re-read stale).
 * Author: Inmo360
 * Version: 1.1.0
 *
 * Instalar en: wp-content/mu-plugins/inmo360-elementor-sync.php
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

add_action(
	'rest_api_init',
	static function () {
		register_rest_route(
			'inmo360/v1',
			'/elementor/rebuild',
			array(
				'methods'             => 'POST',
				'callback'            => 'inmo360_elementor_rebuild',
				'permission_callback' => 'inmo360_elementor_rebuild_permission',
				'args'                => array(
					'page_id' => array(
						'required'          => true,
						'type'              => 'integer',
						'sanitize_callback' => 'absint',
					),
				),
			)
		);
	}
);

/**
 * @param WP_REST_Request $request Request.
 * @return bool|WP_Error
 */
function inmo360_elementor_rebuild_permission( WP_REST_Request $request ) {
	if ( ! is_user_logged_in() ) {
		return new WP_Error(
			'inmo360_unauthorized',
			'Autenticación requerida.',
			array( 'status' => 401 )
		);
	}

	$page_id = absint( $request->get_param( 'page_id' ) );
	if ( $page_id <= 0 ) {
		return new WP_Error(
			'inmo360_invalid_page',
			'page_id inválido.',
			array( 'status' => 400 )
		);
	}

	if ( ! current_user_can( 'edit_post', $page_id ) ) {
		return new WP_Error(
			'inmo360_forbidden',
			'No tiene permisos para editar esta página.',
			array( 'status' => 403 )
		);
	}

	return true;
}

/**
 * @param WP_REST_Request $request Request.
 * @return WP_REST_Response
 */
function inmo360_elementor_rebuild( WP_REST_Request $request ) {
	$page_id = absint( $request->get_param( 'page_id' ) );

	if ( $page_id <= 0 ) {
		return new WP_REST_Response(
			array(
				'ok'      => false,
				'page_id' => $page_id,
				'message' => 'page_id inválido.',
			),
			400
		);
	}

	if ( ! did_action( 'elementor/loaded' ) && ! class_exists( '\Elementor\Plugin' ) ) {
		return new WP_REST_Response(
			array(
				'ok'      => false,
				'page_id' => $page_id,
				'message' => 'Elementor no está cargado.',
			),
			503
		);
	}

	if ( ! class_exists( '\Elementor\Plugin' ) || ! isset( \Elementor\Plugin::$instance->documents ) ) {
		return new WP_REST_Response(
			array(
				'ok'      => false,
				'page_id' => $page_id,
				'message' => 'API de documentos de Elementor no disponible.',
			),
			503
		);
	}

	if ( ! isset( \Elementor\Plugin::$instance->db )
		|| ! method_exists( \Elementor\Plugin::$instance->db, 'get_plain_text_from_data' ) ) {
		return new WP_REST_Response(
			array(
				'ok'      => false,
				'page_id' => $page_id,
				'message' => 'DB::get_plain_text_from_data() no disponible en esta versión de Elementor.',
			),
			503
		);
	}

	try {
		// 1) Invalidar caches antes de leer el documento.
		wp_cache_delete( $page_id, 'post_meta' );
		clean_post_cache( $page_id );

		// 2) Evitar Documents Manager cache (Elementor 4.2.2: get( $id, $from_cache = true )).
		$document = \Elementor\Plugin::$instance->documents->get( $page_id, false );

		if ( ! $document ) {
			return new WP_REST_Response(
				array(
					'ok'      => false,
					'page_id' => $page_id,
					'message' => 'Documento Elementor no encontrado.',
				),
				404
			);
		}

		if ( method_exists( $document, 'is_built_with_elementor' ) && ! $document->is_built_with_elementor() ) {
			return new WP_REST_Response(
				array(
					'ok'      => false,
					'page_id' => $page_id,
					'message' => 'La página no está construida con Elementor.',
				),
				422
			);
		}

		// 3) Elements frescos desde meta.
		$elements = $document->get_elements_data();

		// 4) Validación genérica (sin hardcodear títulos ni page_id de prueba).
		if ( ! is_array( $elements ) || array() === $elements ) {
			return new WP_REST_Response(
				array(
					'ok'      => false,
					'page_id' => $page_id,
					'message' => 'get_elements_data() devolvió datos inválidos o vacíos.',
				),
				500
			);
		}

		// 5) Pipeline oficial Document::save (persiste _elementor_data, CSS delete interno, document cache).
		$saved = $document->save(
			array(
				'elements' => $elements,
			)
		);

		if ( false === $saved ) {
			return new WP_REST_Response(
				array(
					'ok'      => false,
					'page_id' => $page_id,
					'message' => 'Document::save() devolvió false (¿permisos del usuario actual?).',
				),
				403
			);
		}

		// 6) Regenerar post_content desde el MISMO $elements (no re-llamar get_elements_data).
		$plain = \Elementor\Plugin::$instance->db->get_plain_text_from_data( $elements );

		$updated = wp_update_post(
			array(
				'ID'           => $page_id,
				'post_content' => $plain,
			),
			true
		);

		if ( is_wp_error( $updated ) ) {
			return new WP_REST_Response(
				array(
					'ok'      => false,
					'page_id' => $page_id,
					'message' => 'wp_update_post falló: ' . $updated->get_error_message(),
				),
				500
			);
		}

		// 7) CSS del post Elementor.
		if ( class_exists( '\Elementor\Core\Files\CSS\Post' ) ) {
			\Elementor\Core\Files\CSS\Post::create( $page_id )->delete();
		}

		// 8) Cache de archivos Elementor.
		if ( isset( \Elementor\Plugin::$instance->files_manager )
			&& method_exists( \Elementor\Plugin::$instance->files_manager, 'clear_cache' ) ) {
			\Elementor\Plugin::$instance->files_manager->clear_cache();
		}

		// 9) Limpiar cache del post tras actualizar content.
		wp_cache_delete( $page_id, 'post_meta' );
		clean_post_cache( $page_id );

		return new WP_REST_Response(
			array(
				'ok'      => true,
				'page_id' => $page_id,
			),
			200
		);
	} catch ( \Throwable $e ) {
		return new WP_REST_Response(
			array(
				'ok'      => false,
				'page_id' => $page_id,
				'message' => 'Error al guardar documento Elementor: ' . $e->getMessage(),
			),
			500
		);
	}
}
