import PageHeader from '../components/PageHeader'

export default function SimplePage({ title, subtitle }) {
  return (
    <>
      <PageHeader title={title} subtitle={subtitle} />
      <section className="editor-card simple-card">
        <h2>{title}</h2>
        <p>Modulo reservado para a proxima etapa do SIGEM.</p>
      </section>
    </>
  )
}
