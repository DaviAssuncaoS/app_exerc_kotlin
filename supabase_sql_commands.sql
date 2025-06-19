-- Criar uma função para obter a estrutura da tabela
CREATE OR REPLACE FUNCTION get_table_structure(table_name text)
RETURNS json AS $$
BEGIN
    RETURN (
        SELECT json_agg(
            json_build_object(
                'column_name', column_name,
                'data_type', data_type,
                'is_nullable', is_nullable
            )
        )
        FROM information_schema.columns
        WHERE table_name = $1 AND table_schema = 'public'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Conceder permissão para chamar a função
GRANT EXECUTE ON FUNCTION get_table_structure TO anon;